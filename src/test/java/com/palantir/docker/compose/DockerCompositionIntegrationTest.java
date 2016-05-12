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
import static com.palantir.docker.compose.execution.DockerComposeExecArgument.arguments;
import static com.palantir.docker.compose.execution.DockerComposeExecOption.options;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DockerCompositionIntegrationTest {

    private static final List<String> CONTAINERS = ImmutableList.of("db", "db2", "db3", "db4");

    @Rule
    public DockerComposition composition = DockerComposition.of("src/test/resources/docker-compose.yaml")
                                                            .waitingForService("db", toHaveAllPortsOpen())
                                                            .waitingForService("db2", toHaveAllPortsOpen())
                                                            .waitingForServices(ImmutableList.of("db3", "db4"), toAllHaveAllPortsOpen())
                                                            .build();

    private HealthCheck<List<Container>> toAllHaveAllPortsOpen() {
        return containers -> {
            boolean healthy = containers.stream()
                    .map(Container::areAllPortsOpen)
                    .allMatch(SuccessOrFailure::succeeded);

            return SuccessOrFailure.fromBoolean(healthy, "");
        };
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    private void forEachContainer(Consumer<String> consumer) {
        CONTAINERS.forEach(consumer);
    }

    @Test
    public void should_run_docker_compose_up_using_the_specified_docker_compose_file_to_bring_postgres_up() throws InterruptedException, IOException {
        forEachContainer((container) -> {
            try {
                assertThat(composition.portOnContainerWithExternalMapping("db", 5442).isListeningNow(), is(true));
            } catch (IOException | InterruptedException e) {
                propagate(e);
            }
        });
    }

    @Test
    public void after_test_is_executed_the_launched_postgres_container_is_no_longer_listening() throws IOException, InterruptedException {
        composition.after();

        forEachContainer(container -> {
            try {
                assertThat(composition.portOnContainerWithInternalMapping("db", 5432).isListeningNow(), is(false));
            } catch (IOException | InterruptedException e) {
                propagate(e);
            }
        });
    }

    @Test
    public void can_access_external_port_for_internal_port_of_machine() throws IOException, InterruptedException {
        forEachContainer(container -> {
            try {
                assertThat(composition.portOnContainerWithInternalMapping("db", 5432).isListeningNow(), is(true));
            } catch (IOException | InterruptedException e) {
                propagate(e);
            }
        });
    }

    @Ignore // This test will not run on Circle CI because it does not currently support docker-compose exec.
    @Test
    public void exec_returns_output() throws Exception {
        assertThat(composition.exec(options(), CONTAINERS.get(0), arguments("echo", "hello")), is("hello"));
    }

}
