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

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.palantir.docker.compose.DockerComposeManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReportingIntegrationTest {
    @Rule
    public final WireMockRule wireMockRule = new WireMockRule();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    // Run by test
    public static void main(String... args) throws IOException, InterruptedException {
        String testCodeWorkingDir = args[0];

        DockerComposeManager dockerComposeManager = new DockerComposeManager.Builder()
                .file(Paths.get(testCodeWorkingDir, "src/test/resources/no-healthcheck.yaml")
                        .toAbsolutePath()
                        .toString())
                .build();

        try {
            dockerComposeManager.before();
        } finally {
            dockerComposeManager.after();
        }
    }

    @Test
    public void a_report_of_the_right_format_is_posted() throws IOException, InterruptedException {
        File config = temporaryFolder.newFile(".docker-compose-rule.yml");
        Files.write(config.toPath(), ImmutableList.of(
                "reporting:",
                "  url: http://localhost:" + wireMockRule.port() + "/some/path"
        ), StandardCharsets.UTF_8);

        wireMockRule.stubFor(post("/some/path").willReturn(status(200)));

        // We start this in a new java subprocess, as the *intentional* use of shutdown hooks and reading config files
        // from the current working directory makes it very hard to run in process, since other instantiations of
        // DockerComposeManger may have already set statics
        Process process = new ProcessBuilder()
                .command("java",
                        "-classpath",
                        System.getProperty("java.class.path"),
                        ReportingIntegrationTest.class.getCanonicalName(),
                        System.getProperty("user.dir"))
                .directory(temporaryFolder.getRoot())
                .start();

        process.waitFor(30, TimeUnit.SECONDS);

        String stdout = streamToString(process.getInputStream());
        System.out.println("stdout: " + stdout);
        System.out.println("stderr: " + streamToString(process.getErrorStream()));

        wireMockRule.verify(postRequestedFor(urlPathEqualTo("/some/path")));
    }

    private String streamToString(InputStream inputStream) throws IOException {
        return CharStreams.toString(new InputStreamReader(inputStream, UTF_8));
    }
}
