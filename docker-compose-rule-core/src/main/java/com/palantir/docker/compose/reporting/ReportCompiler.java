/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.docker.compose.reporting;

import com.google.common.io.CharStreams;
import com.palantir.docker.compose.DockerComposeManager;
import com.palantir.docker.compose.report.DockerComposeRun;
import com.palantir.docker.compose.report.GitInfo;
import com.palantir.docker.compose.report.Report;
import com.palantir.docker.compose.report.Versions;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import one.util.streamex.EntryStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ReportCompiler implements Reporter {
    private static final Logger log = LoggerFactory.getLogger(ReportCompiler.class);

    private static final String REPORT_API_VERSION = "2";

    private final Clock clock;
    private final PatternCollection environmentVariableWhitelist;
    private final Consumer<Report> reportConsumer;
    private final Report.Builder reportBuilder = Report.builder();

    ReportCompiler(
            Clock clock,
            PatternCollection environmentVariableWhitelist,
            Consumer<Report> reportConsumer) {
        this.clock = clock;
        this.environmentVariableWhitelist = environmentVariableWhitelist;
        this.reportConsumer = reportConsumer;
    }

    @Override
    public synchronized void addRun(DockerComposeRun dockerComposeRun) {
        reportBuilder.runs(dockerComposeRun);
    }

    @Override
    public synchronized void addException(Exception exception) {
        reportBuilder.exceptions(ExceptionUtils.exceptionToString(exception));
    }

    @Override
    public void report() {
        String reportId = IdGenerator.idFor("report");
        log.info("Reporting docker-compose run statistics with id {}", SafeArg.of("reportId", reportId));
        reportConsumer.accept(reportBuilder
                .reportApiVersion(REPORT_API_VERSION)
                .reportId(reportId)
                .submittedTime(clock.instant().atOffset(ZoneOffset.UTC))
                .username(Optional.ofNullable(System.getProperty("user.name")))
                .gitInfo(gitInfo())
                .whitelistedEnvironmentVariables(whitelistedEnvironmentVariables())
                .versions(versions())
                .build());
    }

    private Versions versions() {
        return Versions.builder()
                .dockerComposeRule(versionOf(DockerComposeManager.class))
                .docker(runProcess("docker", "--version"))
                .dockerCompose(runProcess("docker-compose", "--version"))
                .build();
    }

    private Optional<String> versionOf(Class<?> clazz) {
        return Optional.ofNullable(clazz.getPackage().getImplementationVersion());
    }

    private Map<String, String> whitelistedEnvironmentVariables() {
        return EntryStream.of(System.getenv())
                .filterKeys(environmentVariableWhitelist::anyMatch)
                .toMap();
    }

    private GitInfo gitInfo() {
        return GitInfo.builder()
                .branch(runProcess("git", "rev-parse", "--abbrev-ref", "HEAD"))
                .commit(runProcess("git", "rev-parse", "HEAD"))
                .dirty(runProcess("git", "status", "--short").map(output -> !output.isEmpty()))
                .build();
    }

    private Optional<String> runProcess(String... args) {
        try {
            Process process = new ProcessBuilder()
                    .command(args)
                    .start();

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new SafeRuntimeException("Command timed out");
            }

            if (process.exitValue() != 0) {
                throw new SafeRuntimeException("Process exited with exit value {} and stderr:\n{}",
                        SafeArg.of("exitValue", process.exitValue()),
                        SafeArg.of("stderr", inputStreamToString(process.getErrorStream())));
            }

            return Optional.of(inputStreamToString(process.getInputStream()));
        } catch (IOException | InterruptedException | RuntimeException exception) {
            addException(new SafeRuntimeException("Running command failed. Args: {}",
                    exception,
                    SafeArg.of("args", Arrays.asList(args))));
            return Optional.empty();
        }
    }

    private String inputStreamToString(InputStream inputStream) throws IOException {
        return CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).trim();
    }
}
