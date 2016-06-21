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

import com.palantir.docker.compose.execution.DockerExecutionException;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DockerComposeRuleRestartContainersIntegrationTest {

    private static final String DOCKER_COMPOSE_YAML_PATH = "src/test/resources/named-containers-docker-compose.yaml";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void test_docker_compose_rule_fails_with_existing_containers() throws IOException, InterruptedException {
        DockerComposition composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH).build();
        composition.before();
        composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH)
                .removeConflictingContainersOnStartup(false)
                .build();

        exception.expect(DockerExecutionException.class);
        exception.expectMessage("'docker-compose up -d' returned exit code");
        composition.before();
    }

    @Test
    public void test_docker_compose_rule_removes_existing_containers() throws IOException, InterruptedException {
        DockerComposition composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH).build();
        composition.before();

        composition = DockerComposition.of(DOCKER_COMPOSE_YAML_PATH).build();
        composition.before();
        composition.after();
    }

}
