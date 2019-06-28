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
import com.palantir.docker.compose.events.BuildEvent;
import com.palantir.docker.compose.events.ClusterWaitEvent;
import com.palantir.docker.compose.events.DockerComposeRuleEvent;
import com.palantir.docker.compose.events.EventConsumer;
import com.palantir.docker.compose.events.ShutdownEvent;
import com.palantir.docker.compose.events.UpEvent;
import com.palantir.docker.compose.events.WaitForServicesEvent;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("IllegalThrows")
public class EventsIntegrationTest {
    private final EventConsumer eventConsumer = mock(EventConsumer.class);

    @Test
    public void produce_stats_on_a_successful_run() throws Throwable {
        DockerComposeRule dockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/stats/all-good-docker-compose.yaml")
                .waitingForService("one", HealthChecks.toHaveAllPortsOpen())
                .addEventConsumer(eventConsumer)
                .build();

        runDockerComposeRule(dockerComposeRule);

        // InOrder inOrder = inOrder(eventConsumer);
        // inOrder.verify(eventConsumer).receiveEvent();

        List<DockerComposeRuleEvent> events = getEvents();

        List<Class<?>> expected = ImmutableList.of(
                BuildEvent.BuildStarted.class,
                BuildEvent.BuildSucceeded.class,
                UpEvent.UpStarted.class,
                UpEvent.UpSucceeded.class,
                WaitForServicesEvent.WaitForServicesStarted.class,
                ClusterWaitEvent.ClusterStarted.class,
                ClusterWaitEvent.ClusterBecameHealthy.class,
                WaitForServicesEvent.WaitForServicesSucceeded.class,
                ShutdownEvent.ShutdownStarted.class,
                ShutdownEvent.ShutdownSucceeded.class
        );

        assertThat(events).hasSameSizeAs(expected);

        StreamEx.of(events).zipWith(expected.stream())
                .forKeyValue((event, expectedClass) -> {
                    assertThat(event).isInstanceOf(expectedClass);
                });
    }

    @Test
    public void produces_stats_when_a_container_healthcheck_exceeds_its_timeout() {
        // DockerComposeRule dockerComposeRule = DockerComposeRule.builder()
        //         .file("src/test/resources/stats/all-good-docker-compose.yaml")
        //         .waitingForService(
        //                 "one",
        //                 container -> SuccessOrFailure.failure("failed"),
        //                 org.joda.time.Duration.standardSeconds(1))
        //         .addStatsConsumer(eventConsumer)
        //         .build();
        //
        // try {
        //     runDockerComposeRule(dockerComposeRule);
        // } catch (Throwable t) {
        //     // ignore the failure
        // }
        //
        // Stats stats = getEvents();
        //
        // assertThatOptionalDurationIsGreaterThanZero(stats.pullBuildAndStartContainers());
        //
        // assertThat(stats.servicesWithHealthchecksStats()).hasOnlyOneElementSatisfying(containerStats -> {
        //     assertThat(containerStats.serviceName()).isEqualTo("one");
        //     assertThat(containerStats.timeTakenToBecomeHealthy()).isEmpty();
        //     assertThat(containerStats.startedSuccessfully()).isFalse();
        // });
    }

    private List<DockerComposeRuleEvent> getEvents() {
        ArgumentCaptor<DockerComposeRuleEvent> argumentCaptor = ArgumentCaptor.forClass(DockerComposeRuleEvent.class);
        verify(eventConsumer, atLeastOnce()).receiveEvent(argumentCaptor.capture());
        List<DockerComposeRuleEvent> events = argumentCaptor.getAllValues();
        System.out.println("events = " + events);
        return events;
    }

    private void assertThatOptionalDurationIsGreaterThanZero(Optional<Duration> durationValue) {
        assertThat(durationValue).hasValueSatisfying(duration ->
                assertThat(duration).isGreaterThan(Duration.ZERO));
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
