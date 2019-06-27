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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.palantir.docker.compose.connection.waiting.HealthChecks;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import com.palantir.docker.compose.stats.Stats;
import com.palantir.docker.compose.stats.StatsConsumer;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.ArgumentCaptor;

public class StatsIntegrationTest {
    private final StatsConsumer statsConsumer = mock(StatsConsumer.class);

    @SuppressWarnings("IllegalThrows")
    @Test
    public void produce_stats_on_a_successful_run() throws Throwable {
        DockerComposeRule dockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/stats/all-good-docker-compose.yaml")
                .waitingForService("one", HealthChecks.toHaveAllPortsOpen())
                .addStatsConsumer(statsConsumer)
                .build();

        runDockerComposeRule(dockerComposeRule);

        ArgumentCaptor<Stats> argumentCaptor = ArgumentCaptor.forClass(Stats.class);
        verify(statsConsumer).consumeStats(argumentCaptor.capture());

        Stats stats = argumentCaptor.getValue();
        System.out.println("stats = " + stats);
        assertThat(stats.pullBuildAndStartContainers()).isGreaterThan(Duration.ZERO);
        assertThat(stats.forContainersToBecomeHealthy()).isGreaterThan(Duration.ZERO);
        assertThat(stats.containersWithHealthchecksStats()).hasOnlyOneElementSatisfying(containerStats -> {
            assertThat(containerStats.timeTakenToBecomeHealthy()).hasValueSatisfying(duration -> {
                assertThat(duration).isGreaterThan(Duration.ZERO);
            });
            assertThat(containerStats.containerName()).isEqualTo("one");
            assertThat(containerStats.startedSuccessfully()).isTrue();
        });

    }

    @SuppressWarnings("IllegalThrows")
    @Test
    public void produces_stats_when_a_container_healthcheck_exceeds_its_timeout() throws Throwable {
        DockerComposeRule dockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/stats/all-good-docker-compose.yaml")
                .waitingForService(
                        "one",
                        container -> SuccessOrFailure.failure("failed"),
                        org.joda.time.Duration.standardSeconds(1))
                .addStatsConsumer(statsConsumer)
                .build();

        try {
            runDockerComposeRule(dockerComposeRule);
        } catch (Throwable t) {
            // ignore the failure
        }

        ArgumentCaptor<Stats> argumentCaptor = ArgumentCaptor.forClass(Stats.class);
        verify(statsConsumer).consumeStats(argumentCaptor.capture());

        Stats stats = argumentCaptor.getValue();
        System.out.println("stats = " + stats);
        assertThat(stats.pullBuildAndStartContainers()).isGreaterThan(Duration.ZERO);
        assertThat(stats.forContainersToBecomeHealthy()).isGreaterThan(Duration.ZERO);
        assertThat(stats.containersWithHealthchecksStats()).hasOnlyOneElementSatisfying(containerStats -> {
            assertThat(containerStats.containerName()).isEqualTo("one");
            assertThat(containerStats.timeTakenToBecomeHealthy()).isEmpty();
            assertThat(containerStats.startedSuccessfully()).isFalse();
        });

    }

    private void runDockerComposeRule(DockerComposeRule dockerComposeRule) throws Throwable {
        dockerComposeRule.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {

            }
        }, Description.createTestDescription(StatsIntegrationTest.class, "blah"))
                .evaluate();
    }
}
