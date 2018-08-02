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

public class RemoveConflictingContainersIntegrationTest {

    private static final String DOCKER_COMPOSE_YAML_PATH = "src/test/resources/named-containers-docker-compose.yaml";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void removeConflictingContainersOnStartup_off_should_fail_fast_if_containers_exist() throws IOException, InterruptedException {
        DockerComposeRule composition = DockerComposeRule.builder()
                .file(DOCKER_COMPOSE_YAML_PATH)
                .retryAttempts(0)
                .build();
        DockerComposeRule conflictingComposition = DockerComposeRule.builder()
                .file(DOCKER_COMPOSE_YAML_PATH)
                .retryAttempts(0)
                .removeConflictingContainersOnStartup(false)
                .build();
        try {
            composition.before();
            exception.expect(DockerExecutionException.class);
            exception.expectMessage("'docker-compose up -d' returned exit code");
            conflictingComposition.before();
        } finally {
            composition.after();
            conflictingComposition.after();
        }
    }

    @Test
    public void by_default_existing_containers_should_be_removed_silently() throws IOException, InterruptedException {
        DockerComposeRule composition = DockerComposeRule.builder()
                .file(DOCKER_COMPOSE_YAML_PATH)
                .retryAttempts(0)
                .build();
        composition.before();
        composition.before();
        composition.after();
    }

}
