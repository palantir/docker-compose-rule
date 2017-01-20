/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
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

import static com.palantir.docker.compose.configuration.ShutdownStrategy.AGGRESSIVE;
import static com.palantir.docker.compose.connection.waiting.ClusterHealthCheck.serviceHealthCheck;
import static com.palantir.docker.compose.connection.waiting.HealthChecks.toHaveAllPortsOpen;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.State;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import java.io.IOException;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;

public class DockerComposeRuleUpContainerIntegrationTest {

    private static final String SERVICE_NAME = "infinite-netcat-loop";

    @Rule
    public final DockerComposeRule dockerComposeRule = DockerComposeRule
            .builder()
            .shutdownStrategy(AGGRESSIVE)
            .file("src/test/resources/up-integration-test.yaml")
            .build();

    @Test
    public void test_docker_compose_rule_up_container() throws IOException, InterruptedException {
        Container container = dockerComposeRule.containers().container(SERVICE_NAME);

        container.up();

        assertThat(container.state(), is(State.Up));
    }

    @Test
    public void test_docker_compose_rule_up_container_with_healthcheck() throws IOException, InterruptedException {
        Container container = dockerComposeRule.containers().container(SERVICE_NAME);

        container.up();

        // to prove that we can use healthcheck manually after starting a single container
        new ClusterWait(serviceHealthCheck(SERVICE_NAME, toHaveAllPortsOpen()), Duration.standardSeconds(5))
                .waitUntilReady(dockerComposeRule.containers());

        assertThat(container.state(), is(State.Up));
    }
}
