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

import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerComposeExecutionException;
import com.palantir.docker.compose.execution.DockerExecutable;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerComposeRuleRestartContainersIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeRuleRestartContainersIntegrationTest.class);
    private static final String DOCKER_COMPOSE_YAML_PATH = "src/test/resources/named-containers-docker-compose.yaml";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Tests assume that container names in the YAML file are not already in use. In order to enforce this invariant,
     * this cleanup method is run before each test. This method runs "docker rm" on the container names used by the
     * tests. The names are hard-coded and should match the names used by the containers defined in the
     * DOCKER_COMPOSE_YAML_PATH file.
     */
    @Before
    public void cleanup() throws IOException, InterruptedException {
        Docker docker = new Docker(
                DockerExecutable.builder().dockerConfiguration(DockerMachine.localMachine().build()).build());
        try {
            docker.rm("/test-1.container.name", "/test-2.container.name");
        } catch (DockerComposeExecutionException e) {
            log.debug("docker rm failed in cleanup, but continuing", e);
        }
    }

    @Test
    public void testDockerComposeRuleFailsWithExistingContainers() throws IOException, InterruptedException {
        DockerComposition composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH).build();
        composition.before();
        composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH).build();

        exception.expect(DockerComposeExecutionException.class);
        exception.expectMessage("'docker-compose up -d' returned exit code");
        composition.before();
    }

    @Test
    public void testDockerComposeRuleRemovesExistingContainers() throws IOException, InterruptedException {
        DockerComposition composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH).build();
        composition.before();

        composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH)
                .removeConflictingContainersOnStartup(true)
                .build();
        composition.before();
        composition.after();
    }

}
