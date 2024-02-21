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

import com.palantir.docker.compose.events.Event;
import com.palantir.docker.compose.events.EventConsumer;
import com.palantir.docker.compose.report.DockerComposeRun;
import com.palantir.docker.compose.report.TestDescription;
import com.palantir.logsafe.logger.SafeLogger;
import com.palantir.logsafe.logger.SafeLoggerFactory;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;

public final class RunRecorder implements EventConsumer {
    private static final SafeLogger log = SafeLoggerFactory.get(RunRecorder.class);

    private final Clock clock;
    private final Reporter reporter;
    private DockerComposeRun.Builder runBuilder;

    RunRecorder(Clock clock, Reporter reporter) {
        this.clock = clock;
        this.reporter = reporter;

        resetRunBuilder();
    }

    private void resetRunBuilder() {
        runBuilder = DockerComposeRun.builder()
                .runId(IdGenerator.idFor("run"))
                .testDescription(TestDescription.builder().build());
    }

    public void setDescription(TestDescription description) {
        runBuilder.testDescription(description);
    }

    public void before(Callable<String> dockerComposeConfig) {
        runBuilder.startTime(clock.instant().atOffset(ZoneOffset.UTC));
        try {
            runBuilder.dockerComposeConfig(dockerComposeConfig.call());
        } catch (Exception e) {
            runBuilder.exceptions(ExceptionUtils.exceptionToString(e));
            log.error("EnhancedDockerComposeRule has failed in before()", e);
        }
    }

    @Override
    public synchronized void receiveEvent(Event event) {
        runBuilder.events(event);
    }

    public void after() {
        try {
            runBuilder.finishTime(clock.instant().atOffset(ZoneOffset.UTC));
            reporter.addRun(runBuilder.build());
            resetRunBuilder();
        } catch (Exception e) {
            reporter.addException(e);
            log.error("EnhancedDockerComposeRule has failed in after()", e);
        }
    }

    public static RunRecorder defaults() {
        return new RunRecorder(Clock.systemUTC(), PostReportOnShutdown.reporter());
    }
}
