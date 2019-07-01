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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import com.palantir.docker.compose.events.BuildEvent.BuildStarted;
import com.palantir.docker.compose.events.BuildEvent.BuildSucceeded;
import com.palantir.docker.compose.events.ClusterWaitEvent.ClusterBecameHealthy;
import com.palantir.docker.compose.events.ClusterWaitEvent.ClusterStarted;
import com.palantir.docker.compose.events.ClusterWaitEvent.ClusterTimedOut;
import com.palantir.docker.compose.events.DockerComposeRuleEvent;
import com.palantir.docker.compose.events.EventConsumer;
import com.palantir.docker.compose.events.ShutdownEvent.ShutdownStarted;
import com.palantir.docker.compose.events.ShutdownEvent.ShutdownSucceeded;
import com.palantir.docker.compose.events.UpEvent.UpStarted;
import com.palantir.docker.compose.events.UpEvent.UpSucceeded;
import com.palantir.docker.compose.events.WaitForServicesEvent.WaitForServicesStarted;
import com.palantir.docker.compose.events.WaitForServicesEvent.WaitForServicesSucceeded;
import java.util.List;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("IllegalThrows")
public class EventsIntegrationTest {
    private final EventConsumer eventConsumer = mock(EventConsumer.class);

    @Test
    public void produce_events_on_a_successful_run() throws Throwable {
        DockerComposeRule dockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/stats/all-good-docker-compose.yaml")
                .waitingForService("one", HealthChecks.toHaveAllPortsOpen())
                .addEventConsumer(eventConsumer)
                .build();

        runDockerComposeRule(dockerComposeRule);

        List<DockerComposeRuleEvent> events = getEvents();

        List<Class<?>> expected = ImmutableList.of(
                BuildStarted.class,
                BuildSucceeded.class,
                UpStarted.class,
                UpSucceeded.class,
                WaitForServicesStarted.class,
                ClusterStarted.class,
                ClusterStarted.class,
                ClusterStarted.class,
                ClusterBecameHealthy.class,
                ClusterBecameHealthy.class,
                ClusterBecameHealthy.class,
                WaitForServicesSucceeded.class,
                ShutdownStarted.class,
                ShutdownSucceeded.class
        );

        assertThat(events).hasSameSizeAs(expected);

        StreamEx.of(events).zipWith(expected.stream())
                .forKeyValue((event, expectedClass) -> {
                    assertThat(event).isInstanceOf(expectedClass);
                });
    }

    @Test
    public void produces_events_when_a_container_healthcheck_exceeds_its_timeout() {
        String failureMessage = "it went wrong oh no";

        DockerComposeRule dockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/stats/all-good-docker-compose.yaml")
                .waitingForService(
                        "one",
                        container -> SuccessOrFailure.failure(failureMessage),
                        org.joda.time.Duration.standardSeconds(1))
                .addEventConsumer(eventConsumer)
                .build();

        try {
            runDockerComposeRule(dockerComposeRule);
        } catch (Throwable t) {
            // ignore the failure
        }

        List<DockerComposeRuleEvent> events = getEvents();

        assertThat(events).anySatisfy(event -> {
            assertThat(event).isInstanceOf(ClusterTimedOut.class);

            ClusterTimedOut clusterTimedOut = (ClusterTimedOut) event;
            assertThat(clusterTimedOut.serviceNames()).containsOnly("one");
            assertThat(clusterTimedOut.exception()).hasStackTraceContaining(failureMessage);
        });
    }

    private List<DockerComposeRuleEvent> getEvents() {
        ArgumentCaptor<DockerComposeRuleEvent> argumentCaptor = ArgumentCaptor.forClass(DockerComposeRuleEvent.class);
        verify(eventConsumer, atLeastOnce()).receiveEvent(argumentCaptor.capture());
        List<DockerComposeRuleEvent> events = argumentCaptor.getAllValues();
        System.out.println("events = " + events);
        return events;
    }

    private void runDockerComposeRule(DockerComposeRule dockerComposeRule) throws Throwable {
        dockerComposeRule.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {

            }
        }, Description.createTestDescription(EventsIntegrationTest.class, "blah"))
                .evaluate();
    }
}
