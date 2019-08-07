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
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RunRecorder implements EventConsumer {
    private static final Logger log = LoggerFactory.getLogger(RunRecorder.class);

    private final Clock clock;
    private final Reporter reporter;
    private final DockerComposeRun.Builder runBuilder = DockerComposeRun.builder();

    RunRecorder(
            Clock clock,
            Reporter reporter) {
        this.clock = clock;
        this.reporter = reporter;

        runBuilder.runId(IdGenerator.idFor("run"));
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
    public void receiveEvent(Event event) {
        runBuilder.events(event);
    }

    public void after() {
        try {
            runBuilder.finishTime(clock.instant().atOffset(ZoneOffset.UTC));
            reporter.addRun(runBuilder.build());
        } catch (Exception e) {
            reporter.addException(e);
            log.error("EnhancedDockerComposeRule has failed in after()", e);
        }
    }

    public static RunRecorder defaults() {
        return new RunRecorder(Clock.systemUTC(), PostReportOnShutdown.reporter());
    }
}
