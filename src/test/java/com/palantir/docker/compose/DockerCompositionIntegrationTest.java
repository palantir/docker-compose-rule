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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DockerCompositionIntegrationTest {

    @Rule
    public DockerComposition composition = DockerComposition.of("src/test/resources/docker-compose.yaml")
                                                            .waitingForService("db")
                                                            .waitingForService("db2")
                                                            .build();

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    @Test
    public void should_run_docker_compose_up_using_the_specified_docker_compose_file_to_bring_postgres_up() throws InterruptedException, IOException {
        assertThat(composition.portOnContainerWithExternalMapping("db", 5433).isListeningNow(), is(true));
    }

    @Test
    public void after_test_is_executed_the_launched_postgres_container_is_no_longer_listening() throws IOException, InterruptedException {
        composition.after();
        assertThat(composition.portOnContainerWithExternalMapping("db", 5433).isListeningNow(), is(false));
    }

    @Test
    public void can_access_external_port_for_internal_port_of_machine() throws IOException, InterruptedException {
        assertThat(composition.portOnContainerWithInternalMapping("db", 5432).isListeningNow(), is(true));
    }

}
