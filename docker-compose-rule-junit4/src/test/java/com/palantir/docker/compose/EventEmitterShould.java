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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.events.BuildEvent;
import com.palantir.docker.compose.events.ClusterWaitEvent.ClusterBecameHealthy;
import com.palantir.docker.compose.events.ClusterWaitEvent.ClusterStarted;
import com.palantir.docker.compose.events.DockerComposeRuleEvent;
import com.palantir.docker.compose.events.EventConsumer;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class EventEmitterShould {
    private final EventConsumer eventConsumer1 = mock(EventConsumer.class);
    private final EventConsumer eventConsumer2 = mock(EventConsumer.class);
    private final EventEmitter eventEmitter = new EventEmitter(ImmutableList.of(eventConsumer1, eventConsumer2));

    private final InOrder inOrder = Mockito.inOrder(eventConsumer1, eventConsumer2);

    @Test
    public void produce_started_then_succeeded_for_successful_build_run() throws IOException, InterruptedException {
        eventEmitter.build(() -> {
            inOrder.verify(eventConsumer1).receiveEvent(BuildEvent.FACTORY.started());
            inOrder.verify(eventConsumer2).receiveEvent(BuildEvent.FACTORY.started());
        });

        inOrder.verify(eventConsumer1).receiveEvent(BuildEvent.FACTORY.succeeded());
        inOrder.verify(eventConsumer2).receiveEvent(BuildEvent.FACTORY.succeeded());
    }

    @Test
    public void produce_started_then_failed_for_failing_build_run() {
        IllegalStateException exception = new IllegalStateException("nooooo");

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
                eventEmitter.build(() -> {
                    inOrder.verify(eventConsumer1).receiveEvent(BuildEvent.FACTORY.started());
                    inOrder.verify(eventConsumer2).receiveEvent(BuildEvent.FACTORY.started());
                    throw exception;
                })
        );

        inOrder.verify(eventConsumer1).receiveEvent(BuildEvent.FACTORY.failed(exception));
        inOrder.verify(eventConsumer2).receiveEvent(BuildEvent.FACTORY.failed(exception));
    }

    @Test
    public void user_cluster_wait_give_the_service_names_that_were_used_by_the_cluser_wait_in_an_event() {
        ClusterWait clusterWait = mock(ClusterWait.class);
        doAnswer(invocation -> {
            Cluster cluster = (Cluster) invocation.getArguments()[0];
            cluster.container("one");
            cluster.containers(ImmutableList.of("two", "three"));
            return null;
        }).when(clusterWait).waitUntilReady(any());

        Consumer<Cluster> eventedClusterWait = eventEmitter.userClusterWait(clusterWait);

        Cluster cluster = mock(Cluster.class, RETURNS_DEEP_STUBS);
        eventedClusterWait.accept(cluster);

        List<DockerComposeRuleEvent> events = getEvents();

        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(ClusterStarted.class);
        assertThat(events.get(1)).isInstanceOf(ClusterBecameHealthy.class);

        ClusterStarted clusterStarted = (ClusterStarted) events.get(0);
        ClusterBecameHealthy clusterBecameHealthy = (ClusterBecameHealthy) events.get(1);

        assertThat(clusterStarted.eventId()).isEqualTo(clusterBecameHealthy.eventId());
        assertThat(clusterBecameHealthy.serviceNames()).containsExactly("one", "two", "three");
    }

    private List<DockerComposeRuleEvent> getEvents() {
        ArgumentCaptor<DockerComposeRuleEvent> eventsCapture = ArgumentCaptor.forClass(DockerComposeRuleEvent.class);
        verify(eventConsumer1, atLeastOnce()).receiveEvent(eventsCapture.capture());
        return eventsCapture.getAllValues();
    }
}
