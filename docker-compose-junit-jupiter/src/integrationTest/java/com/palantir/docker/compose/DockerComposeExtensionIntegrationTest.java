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

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DockerComposeExtensionIntegrationTest {

    @RegisterExtension
    public static final DockerComposeExtension docker = DockerComposeExtension.builder()
            .files(DockerComposeFiles.from("src/integrationTest/resources/docker-compose.yaml"))
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db2", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db3", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db4", HealthChecks.toHaveAllPortsOpen())
            .build();

    private static int port;

    @Test
    @Order(1)
    public void an_external_port_exists() {
        port = docker.containers().container("db").port(5432).getExternalPort();
        assertThat(port).isNotZero();
    }

    @Test
    @Order(2)
    public void container_stays_up_between_tests() {
        assertThat(docker.containers().container("db").port(5432).getExternalPort())
                .isEqualTo(port);
    }
}
