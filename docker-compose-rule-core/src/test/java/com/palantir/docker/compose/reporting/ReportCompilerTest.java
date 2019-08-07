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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.report.DockerComposeRun;
import com.palantir.docker.compose.report.Report;
import com.palantir.docker.compose.report.TestDescription;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ReportCompilerTest {
    private final Clock clock = mock(Clock.class);
    private final Consumer<Report> reportConsumer = mock(Consumer.class);
    private final ReportCompiler reporter = new ReportCompiler(clock, reportConsumer);

    @Test
    public void can_collect_then_post_runs() {
        Instant time = Instant.ofEpochSecond(1);
        when(clock.instant()).thenReturn(time);

        DockerComposeRun run = DockerComposeRun.builder()
                .runId("runId")
                .startTime(OffsetDateTime.MIN)
                .finishTime(OffsetDateTime.MAX)
                .testDescription(TestDescription.builder().build())
                .build();
        RuntimeException exception = new RuntimeException("oh no");

        reporter.addRun(run);
        reporter.addRun(run);
        reporter.addException(exception);

        reporter.report();

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportConsumer).accept(captor.capture());
        Report sentReport = captor.getValue();

        assertThat(sentReport.getRuns()).containsOnly(run, run);
        assertThat(sentReport.getExceptions()).hasOnlyOneElementSatisfying(exceptionString -> {
            assertThat(exceptionString).contains(exception.getMessage());
        });
        assertThat(sentReport.getSubmittedTime()).isEqualTo(time.atOffset(ZoneOffset.UTC));
    }

}
