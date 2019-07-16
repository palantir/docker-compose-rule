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

package com.palantir.docker.compose;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.EventEmitter.InterruptableClusterWait;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.connection.waiting.Exceptions;
import com.palantir.docker.compose.events.BuildEvent;
import com.palantir.docker.compose.events.ClusterWaitEvent;
import com.palantir.docker.compose.events.ClusterWaitType;
import com.palantir.docker.compose.events.Event;
import com.palantir.docker.compose.events.EventConsumer;
import com.palantir.docker.compose.events.Task;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class EventEmitterShould {
    private final Clock clock = mock(Clock.class);
    private final EventConsumer eventConsumer1 = mock(EventConsumer.class);
    private final EventConsumer eventConsumer2 = mock(EventConsumer.class);
    private final EventEmitter eventEmitter = new EventEmitter(clock, ImmutableList.of(eventConsumer1, eventConsumer2));

    private final InOrder inOrder = Mockito.inOrder(eventConsumer1, eventConsumer2);

    @Test
    public void produce_started_then_succeeded_for_successful_build_run() throws IOException, InterruptedException {
        OffsetDateTime startedTime = timeIs(5);
        AtomicReference<OffsetDateTime> endTime = new AtomicReference<>();

        eventEmitter.build(() -> {
            endTime.set(timeIs(10));
        });

        Event succeededBuild = Event.build(BuildEvent.builder()
                .task(Task.builder()
                        .startTime(startedTime)
                        .endTime(endTime.get())
                        .build())
                .build());

        inOrder.verify(eventConsumer1).receiveEvent(succeededBuild);
        inOrder.verify(eventConsumer2).receiveEvent(succeededBuild);
    }

    @Test
    public void produce_started_then_failed_for_failing_build_run() {
        IllegalStateException exception = new IllegalStateException("nooooo");

        OffsetDateTime startedTime = timeIs(5);
        AtomicReference<OffsetDateTime> endTime = new AtomicReference<>();

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
                eventEmitter.build(() -> {
                    endTime.set(timeIs(10));
                    throw exception;
                })
        );

        Event failedBuild = Event.build(BuildEvent.builder()
                .task(Task.builder()
                        .startTime(startedTime)
                        .endTime(endTime.get())
                        .failure(Exceptions.condensedStacktraceFor(exception))
                        .build())
                .build());

        inOrder.verify(eventConsumer1).receiveEvent(failedBuild);
        inOrder.verify(eventConsumer2).receiveEvent(failedBuild);
    }

    @Test
    public void user_cluster_wait_give_the_service_names_that_were_used_by_the_cluser_wait_in_an_event()
            throws InterruptedException {
        OffsetDateTime startedTime = timeIs(5);
        AtomicReference<OffsetDateTime> endTime = new AtomicReference<>();

        ClusterWait clusterWait = mock(ClusterWait.class);
        doAnswer(invocation -> {
            Cluster cluster = (Cluster) invocation.getArguments()[0];
            cluster.container("one");
            cluster.containers(ImmutableList.of("two", "three"));
            endTime.set(timeIs(20));
            return null;
        }).when(clusterWait).waitUntilReady(any());

        InterruptableClusterWait eventedClusterWait = eventEmitter.userClusterWait(clusterWait);

        Cluster cluster = mock(Cluster.class, RETURNS_DEEP_STUBS);
        eventedClusterWait.waitForCluster(cluster);

        Event clusterWaitEvent = Event.clusterWait(ClusterWaitEvent.builder()
                .task(Task.builder()
                        .startTime(startedTime)
                        .endTime(endTime.get())
                        .build())
                .type(ClusterWaitType.USER)
                .serviceNames(ImmutableList.of("one", "two", "three"))
                .build());

        inOrder.verify(eventConsumer1).receiveEvent(clusterWaitEvent);
        inOrder.verify(eventConsumer2).receiveEvent(clusterWaitEvent);
    }

    @Test
    public void return_all_exceptions_as_suppressed() throws IOException, InterruptedException {
        timeIs(5);

        RuntimeException one = new RuntimeException("one");
        RuntimeException two = new RuntimeException("two");
        doThrow(one).when(eventConsumer1).receiveEvent(any());
        doThrow(two).when(eventConsumer2).receiveEvent(any());

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
            eventEmitter.build(() -> { });
        }).satisfies(runtimeException -> {
            assertThat(runtimeException).hasSuppressedException(one);
            assertThat(runtimeException).hasSuppressedException(two);
        });
    }

    private OffsetDateTime timeIs(int seconds) {
        Instant instant = Instant.ofEpochSecond(seconds);
        when(clock.instant()).thenReturn(instant);
        return instant.atOffset(ZoneOffset.UTC);
    }
}
