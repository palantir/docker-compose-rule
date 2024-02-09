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
package com.palantir.docker.compose.execution;

import static com.palantir.docker.compose.connection.waiting.HealthChecks.toHaveAllPortsOpen;
import static com.palantir.docker.compose.matchers.IoMatchers.fileContainingString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import com.palantir.docker.compose.DockerComposeManager;
import com.palantir.docker.compose.connection.DockerMachine;
import java.nio.file.Path;
import org.assertj.core.api.HamcrestCondition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EnvironmentVariableIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void docker_compose_gets_environment_variables_from_docker_machine_and_passes_it_into_a_test_container()
            throws Exception {
        DockerMachine dockerMachine = DockerMachine.localMachine()
                .withAdditionalEnvironmentVariable("SOME_VARIABLE", "SOME_VALUE")
                .build();

        DockerComposeManager dockerComposition = new DockerComposeManager.Builder()
                .file("src/test/resources/environment/docker-compose.yaml")
                .machine(dockerMachine)
                .waitingForService("env-test", toHaveAllPortsOpen())
                .saveLogsTo(temporaryFolder.getRoot().getAbsolutePath())
                .build();

        try {
            dockerComposition.before();
        } finally {
            dockerComposition.after();
        }

        Path logLocation = temporaryFolder.getRoot().toPath().resolve("env-test.log");

        assertThat(logLocation.toFile())
                .is(new HamcrestCondition<>(is(fileContainingString("SOME_VARIABLE=SOME_VALUE"))));
    }
}
