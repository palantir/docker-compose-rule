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

import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RemoteBuilderShould {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void throw_exception_when_building_a_docker_machine_without_a_host() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables");
        exception.expectMessage("DOCKER_HOST");
        DockerMachine.remoteMachine()
                     .withoutTLS()
                     .build();
    }

    @Test
    public void have_no_tls_environment_variables_when_a_docker_machine_is_built_without_tls() throws Exception {
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
    public void have_tls_environment_variables_set_when_a_docker_machine_is_built_with_tls() throws Exception {
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
    public void build_a_docker_machine_with_additional_environment_variables() throws Exception {
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
