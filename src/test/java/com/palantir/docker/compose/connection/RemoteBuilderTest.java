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
package com.palantir.docker.compose.connection;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

public class RemoteBuilderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void a_docker_machine_built_without_a_host_results_in_an_error() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables");
        exception.expectMessage("DOCKER_HOST");
        DockerMachine.remoteMachine()
                     .withoutTLS()
                     .build();
    }

    @Test
    public void a_docker_machine_built_without_t_l_s_has_no_t_l_s_environment_variables() throws Exception {
        DockerMachine dockerMachine = DockerMachine.remoteMachine()
                                                   .host("tcp://192.168.99.100")
                                                   .withoutTLS()
                                                   .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                                                   .put(DOCKER_HOST, "tcp://192.168.99.100")
                                                   .build();

        validateEnvironmentConfiguredDirectly(dockerMachine, expected);
    }

    @Test
    public void a_docker_machine_built_with_t_l_s_has_t_l_s_environment_variables_set() throws Exception {
        DockerMachine dockerMachine = DockerMachine.remoteMachine()
                                                   .host("tcp://192.168.99.100")
                                                   .withTLS("/path/to/certs")
                                                   .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();
        validateEnvironmentConfiguredDirectly(dockerMachine, expected);
    }

    @Test
    public void a_docker_machine_built_with_additional_environment_variables() throws Exception {
        DockerMachine dockerMachine = DockerMachine.remoteMachine()
                                                   .host("tcp://192.168.99.100")
                                                   .withoutTLS()
                                                   .withAdditionalEnvironmentVariable("SOME_VARIABLE", "SOME_VALUE")
                                                   .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100")
                .put("SOME_VARIABLE", "SOME_VALUE")
                .build();
        validateEnvironmentConfiguredDirectly(dockerMachine, expected);
    }

    private void validateEnvironmentConfiguredDirectly(DockerMachine dockerMachine, Map<String, String> expectedEnvironment) {
        ProcessBuilder process = dockerMachine.configuredDockerComposeProcess();

        Map<String, String> environment = process.environment();
        expectedEnvironment.forEach((var, val) -> assertThat(environment, hasEntry(var, val)));
    }

}