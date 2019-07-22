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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import com.palantir.docker.compose.events.BuildEvent;
import com.palantir.docker.compose.events.ClusterWaitEvent;
import com.palantir.docker.compose.events.Event;
import com.palantir.docker.compose.events.EventConsumer;
import com.palantir.docker.compose.events.LogCollectionEvent;
import com.palantir.docker.compose.events.PullEvent;
import com.palantir.docker.compose.events.ShutdownEvent;
import com.palantir.docker.compose.events.ShutdownStopEvent;
import com.palantir.docker.compose.events.UpEvent;
import com.palantir.docker.compose.events.WaitForServicesEvent;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("IllegalThrows")
public class EventsIntegrationTest {
    public static final String ALL_GOOD_DOCKER_COMPOSE_YAML = "src/test/resources/events/all-good-docker-compose.yaml";
    private final EventConsumer eventConsumer = mock(EventConsumer.class);

    @Test
    public void produce_events_on_a_successful_run() throws Throwable {
        DockerComposeManager dockerComposeManager = new DockerComposeManager.Builder()
                .file(ALL_GOOD_DOCKER_COMPOSE_YAML)
                .waitingForService("one", HealthChecks.toHaveAllPortsOpen())
                .addEventConsumer(eventConsumer)
                .build();

        runDockerComposeRule(dockerComposeManager);

        List<Event> events = getEvents();

        List<Class<?>> expected = ImmutableList.of(
                BuildEvent.class,
                UpEvent.class,
                // 2 cluster waits, one native, one user
                ClusterWaitEvent.class,
                ClusterWaitEvent.class,
                WaitForServicesEvent.class,
                ShutdownStopEvent.class,
                LogCollectionEvent.class,
                ShutdownEvent.class
        );

        assertThat(events).hasSameSizeAs(expected);
        for (int i = 0; i < events.size(); i++) {
            assertThat(events.get(i).toString()).contains(expected.get(i).getSimpleName());
        }
    }

    @Test
    public void produces_events_when_a_container_healthcheck_exceeds_its_timeout() {
        String failureMessage = "it went wrong oh no";

        DockerComposeManager dockerComposeManager = new DockerComposeManager.Builder()
                .file(ALL_GOOD_DOCKER_COMPOSE_YAML)
                .waitingForService(
                        "one",
                        container -> SuccessOrFailure.failure(failureMessage),
                        org.joda.time.Duration.standardSeconds(1))
                .addEventConsumer(eventConsumer)
                .build();

        try {
            runDockerComposeRule(dockerComposeManager);
            fail("Was expecting an exception");
        } catch (Throwable t) {
            // ignore the failure
        }

        List<Event> events = getEvents();

        ClusterWaitEvent clusterWait = events.stream()
                .map(this::isClusterWait)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no clusterwaits in events"));


        assertThat(clusterWait.getServiceNames()).containsOnly("one");
        assertThat(clusterWait.getTask().getFailure()).hasValueSatisfying(failure -> {
            assertThat(failure).contains(failureMessage);
        });
    }

    private List<Event> getEvents() {
        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventConsumer, atLeastOnce()).receiveEvent(argumentCaptor.capture());
        List<Event> events = argumentCaptor.getAllValues();
        System.out.println("events = " + events);
        return events;
    }

    private void runDockerComposeRule(DockerComposeManager dockerComposeManager) throws Throwable {
        try {
            dockerComposeManager.before();
        } finally {
            dockerComposeManager.after();
        }
    }

    private Optional<ClusterWaitEvent> isClusterWait(Event event) {
        return event.accept(new Event.Visitor<Optional<ClusterWaitEvent>>() {
            @Override
            public Optional<ClusterWaitEvent> visitBuild(BuildEvent value) {
                return Optional.empty();
            }

            @Override
            public Optional<ClusterWaitEvent> visitPull(PullEvent value) {
                return Optional.empty();
            }

            @Override
            public Optional<ClusterWaitEvent> visitUp(UpEvent value) {
                return Optional.empty();
            }

            @Override
            public Optional<ClusterWaitEvent> visitWaitForServices(WaitForServicesEvent value) {
                return Optional.empty();
            }

            @Override
            public Optional<ClusterWaitEvent> visitClusterWait(ClusterWaitEvent value) {
                return Optional.of(value);
            }

            @Override
            public Optional<ClusterWaitEvent> visitShutdownStop(ShutdownStopEvent value) {
                return Optional.empty();
            }

            @Override
            public Optional<ClusterWaitEvent> visitLogCollection(LogCollectionEvent value) {
                return Optional.empty();
            }

            @Override
            public Optional<ClusterWaitEvent> visitShutdown(ShutdownEvent value) {
                return Optional.empty();
            }

            @Override
            public Optional<ClusterWaitEvent> visitUnknown(String unknownType) {
                return Optional.empty();
            }
        });
    }
}
