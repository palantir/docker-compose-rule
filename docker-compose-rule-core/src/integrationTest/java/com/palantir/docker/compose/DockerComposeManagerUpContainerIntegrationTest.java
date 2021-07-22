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

import static com.palantir.docker.compose.configuration.ShutdownStrategy.AGGRESSIVE;
import static com.palantir.docker.compose.connection.waiting.ClusterHealthCheck.serviceHealthCheck;
import static com.palantir.docker.compose.connection.waiting.HealthChecks.toHaveAllPortsOpen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.State;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import java.io.IOException;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;

public class DockerComposeManagerUpContainerIntegrationTest {

    private static final String SERVICE_NAME = "infinite-netcat-loop";

    private DockerComposeManager dockerComposeManager;

    @Before
    public void before() throws Exception {
        dockerComposeManager = new DockerComposeManager.Builder()
                .shutdownStrategy(AGGRESSIVE)
                .file("src/test/resources/up-integration-test.yaml")
                .build();
        dockerComposeManager.before();
    }

    @Test
    public void test_docker_compose_manager_up_container() throws IOException, InterruptedException {
        Container container = dockerComposeManager.containers().container(SERVICE_NAME);

        container.up();

        assertThat(container.state()).isEqualTo(State.HEALTHY);
    }

    @Test
    public void test_docker_compose_manager_up_container_with_healthcheck() throws IOException, InterruptedException {
        Container container = dockerComposeManager.containers().container(SERVICE_NAME);

        container.up();

        // to prove that we can use healthcheck manually after starting a single container
        new ClusterWait(serviceHealthCheck(SERVICE_NAME, toHaveAllPortsOpen()), Duration.standardSeconds(5))
                .waitUntilReady(dockerComposeManager.containers());

        assertThat(container.state()).isEqualTo(State.HEALTHY);
    }
}
