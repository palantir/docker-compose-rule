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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.palantir.docker.compose.events.BuildEvent;
import com.palantir.docker.compose.events.Event;
import com.palantir.docker.compose.events.PullEvent;
import com.palantir.docker.compose.events.Task;
import com.palantir.docker.compose.report.DockerComposeRun;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class RunRecorderTest {
    private static final Instant THE_TIME = Instant.ofEpochSecond(1);
    private static final OffsetDateTime DATE_TIME = THE_TIME.atOffset(ZoneOffset.UTC);

    private static final Task TASK = Task.builder()
            .startTime(DATE_TIME)
            .endTime(DATE_TIME)
            .build();

    private static final Event BUILD_EVENT = Event.build(BuildEvent.builder()
            .task(TASK)
            .build());

    private static final Event PULL_EVENT = Event.pull(PullEvent.builder()
            .task(TASK)
            .build());

    private final Reporter reporter = mock(Reporter.class);
    private final RunRecorder runRecorder = new RunRecorder(
            Clock.fixed(THE_TIME, ZoneId.of("UTC")),
            reporter);

    @Test
    public void will_not_duplicate_data_when_run_twice() {
        runRecorder.before(() -> "dcr1");
        runRecorder.receiveEvent(BUILD_EVENT);
        runRecorder.after();

        runRecorder.before(() -> "dcr2");
        runRecorder.receiveEvent(PULL_EVENT);
        runRecorder.after();

        ArgumentCaptor<DockerComposeRun> runCaptor = ArgumentCaptor.forClass(DockerComposeRun.class);
        verify(reporter, times(2)).addRun(runCaptor.capture());

        List<DockerComposeRun> runs = runCaptor.getAllValues();
        assertThat(runs.get(0).getDockerComposeConfig()).hasValue("dcr1");
        assertThat(runs.get(0).getEvents()).containsExactly(BUILD_EVENT);

        assertThat(runs.get(1).getDockerComposeConfig()).hasValue("dcr2");
        assertThat(runs.get(1).getEvents()).containsExactly(PULL_EVENT);
    }
}
