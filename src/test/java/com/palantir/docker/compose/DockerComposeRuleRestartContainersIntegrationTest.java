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

import com.palantir.docker.compose.execution.DockerComposeExecutionException;
import java.io.IOException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Ignore // these tests will not run on Circle CI because it does not currently support docker rm
public class DockerComposeRuleRestartContainersIntegrationTest {

    private static final String DOCKER_COMPOSE_YAML_PATH = "src/test/resources/named-containers-docker-compose.yaml";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Tests assume that container names in the YAML file are not already in use. In order to enforce this invariant,
     * this cleanup method is run before each test. This method runs docker-compose up for the docker-compose file,
     * removes any containers with conflicting names, and then shuts down (and removes the created containers). This
     * ensures that the containers do not exist before the tests are run.
     */
    @Before
    public void cleanup() throws IOException, InterruptedException {
        DockerComposition composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH)
                .removeConflictingContainersOnStartup(true)
                .build();
        composition.before();
        composition.after();
    }

    @Test
    public void testDockerComposeRuleFailsWithExistingContainers() throws IOException, InterruptedException {
        DockerComposition composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH).build();
        composition.before();

        composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH).build();

        exception.expect(DockerComposeExecutionException.class);
        exception.expectMessage("'docker-compose up -d' returned exit code 255");
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
