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

import static com.google.common.base.Throwables.propagate;
import static com.palantir.docker.compose.connection.waiting.HealthChecks.toHaveAllPortsOpen;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.execution.DockerExecutionException;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DockerComposeRulePartialServicesIntegrationTest {

    private static final List<String> CONTAINERS = ImmutableList.of("db", "db2", "db3", "db4");

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public final DockerComposeRule docker = DockerComposeRule.builder()
            .files(DockerComposeFiles.from("src/test/resources/docker-compose.yaml"))
            .addServicesToStart("db") // Only start the db container.
            .waitingForService("db", toHaveAllPortsOpen())
            .build();

    // Not a @Rule so that we can call before() and handle the exception.
    // db5 is a nonexistent service in docker-compose.yaml.
    private final DockerComposeRule invalidDocker = DockerComposeRule.builder()
            .files(DockerComposeFiles.from("src/test/resources/docker-compose.yaml"))
            .addServicesToStart("db5") // Only start the db container.
            .waitingForService("db5", toHaveAllPortsOpen())
            .build();

    private static void forEachContainer(Consumer<String> consumer) {
        CONTAINERS.forEach(consumer);
    }

    @Test
    public void should_run_docker_compose_up_for_db_container_only() {
        forEachContainer(containerName -> {
            Container container = docker.containers().container(containerName);
            try {
                if ("db".equals(containerName)) {
                    assertThat(container.state().isUp(), is(true));
                    assertThat(container.port(5432).isListeningNow(), is(true));
                } else {
                    assertThat(container.state().isUp(), is(false));
                }
            } catch (IOException | InterruptedException e) {
                propagate(e);
            }
        });
    }

    @Test
    public void after_test_is_executed_the_launched_db_container_is_no_longer_listening() {
        docker.after();

        forEachContainer(containerName -> {
            Container container = docker.containers().container(containerName);
            try {
                assertThat(container.state().isUp(), is(false));
                if ("db".equals(containerName)) {
                    assertThat(container.port(5432).isListeningNow(), is(false));
                }
            } catch (IOException | InterruptedException e) {
                propagate(e);
            }
        });
    }

    @Test
    public void should_run_docker_compose_up_for_nonexistent_container() throws IOException, InterruptedException {
        exception.expect(DockerExecutionException.class);
        exception.expectMessage("No such service: db5");

        invalidDocker.before();
    }
}
