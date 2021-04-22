/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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

import static com.google.common.collect.Maps.newHashMap;
import static com.palantir.docker.compose.configuration.DaemonHostIpResolver.LOCALHOST;
import static com.palantir.docker.compose.configuration.DockerType.DAEMON;
import static com.palantir.docker.compose.configuration.DockerType.REMOTE;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;
import static com.palantir.docker.compose.matchers.DockerMachineEnvironmentMatcher.containsEnvironment;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.connection.DockerMachine.LocalBuilder;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LocalBuilderShould {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void override_previous_environment_when_additional_environment_set_twice_daemon() {
        Map<String, String> environment1 = ImmutableMap.of("ENV_1", "VAL_1");
        Map<String, String> environment2 = ImmutableMap.of("ENV_2", "VAL_2");
        DockerMachine localMachine = new LocalBuilder(DAEMON, new HashMap<>()).withEnvironment(environment1)
                                                                           .withEnvironment(environment2)
                                                                           .build();
        assertThat(localMachine, not(containsEnvironment(environment1)));
        assertThat(localMachine, containsEnvironment(environment2));
    }

    @Test
    public void be_union_of_additional_environment_and_individual_environment_when_both_set_daemon() {
        Map<String, String> environment = ImmutableMap.<String, String>builder()
                                                       .put("ENV_1", "VAL_1")
                                                       .put("ENV_2", "VAL_2")
                                                       .build();
        DockerMachine localMachine = new LocalBuilder(DAEMON, new HashMap<>()).withEnvironment(environment)
                                                                           .withAdditionalEnvironmentVariable("ENV_3", "VAL_3")
                                                                           .build();
        assertThat(localMachine, containsEnvironment(environment));
        assertThat(localMachine, containsEnvironment(ImmutableMap.of("ENV_3", "VAL_3")));
    }

    @Test
    public void override_previous_environment_with_additional_environment_set_twice_remote() {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .build();
        Map<String, String> environment1 = ImmutableMap.of("ENV_1", "VAL_1");
        Map<String, String> environment2 = ImmutableMap.of("ENV_2", "VAL_2");
        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).withEnvironment(environment1)
                                                                           .withEnvironment(environment2)
                                                                           .build();
        assertThat(localMachine, not(containsEnvironment(environment1)));
        assertThat(localMachine, containsEnvironment(environment2));
    }

    @Test
    public void be_union_of_additional_environment_and_individual_environment_when_both_set_remote() {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .build();
        Map<String, String> environment = ImmutableMap.<String, String>builder()
                .put("ENV_1", "VAL_1")
                .put("ENV_2", "VAL_2")
                .build();
        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).withEnvironment(environment)
                                                                              .withAdditionalEnvironmentVariable("ENV_3", "VAL_3")
                                                                              .build();
        assertThat(localMachine, containsEnvironment(environment));
        assertThat(localMachine, containsEnvironment(ImmutableMap.of("ENV_3", "VAL_3")));
    }

    @Test
    public void get_variable_overriden_with_additional_environment() {
        Map<String, String> environment = ImmutableMap.<String, String>builder()
                .put("ENV_1", "VAL_1")
                .put("ENV_2", "VAL_2")
                .build();
        DockerMachine localMachine = new LocalBuilder(DAEMON, new HashMap<>()).withEnvironment(environment)
                                                                           .withAdditionalEnvironmentVariable("ENV_2", "DIFFERENT_VALUE")
                                                                           .build();

        Map<String, String> expected = ImmutableMap.<String, String>builder()
                .put("ENV_1", "VAL_1")
                .put("ENV_2", "DIFFERENT_VALUE")
                .build();
        assertThat(localMachine, not(containsEnvironment(environment)));
        assertThat(localMachine, containsEnvironment(expected));
    }

    @Test
    public void override_system_environment_with_additional_environment() {
        Map<String, String> systemEnv = ImmutableMap.<String, String>builder()
                .put("ENV_1", "VAL_1")
                .build();
        Map<String, String> overrideEnv = ImmutableMap.<String, String>builder()
                .put("ENV_1", "DIFFERENT_VALUE")
                .build();
        DockerMachine localMachine = new LocalBuilder(DAEMON, systemEnv)
                .withEnvironment(overrideEnv)
                .build();

        assertThat(localMachine, not(containsEnvironment(systemEnv)));
        assertThat(localMachine, containsEnvironment(overrideEnv));
    }

    @Test
    public void have_invalid_variables_daemon() {
        Map<String, String> invalidDockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("These variables were set");
        exception.expectMessage(DOCKER_HOST);
        exception.expectMessage(DOCKER_CERT_PATH);
        exception.expectMessage(DOCKER_TLS_VERIFY);
        exception.expectMessage("They cannot be set when connecting to a local docker daemon");

        new LocalBuilder(DAEMON, invalidDockerVariables).build();
    }

    @Test
    public void have_invalid_additional_variables_daemon() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("The following variables");
        exception.expectMessage(DOCKER_HOST);
        exception.expectMessage("cannot exist in your additional environment variable block");

        new LocalBuilder(DAEMON, new HashMap<>()).withAdditionalEnvironmentVariable(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                              .build();
    }

    @Test
    public void have_invalid_additional_variables_remote() {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                                                          .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                                          .put(DOCKER_TLS_VERIFY, "1")
                                                          .put(DOCKER_CERT_PATH, "/path/to/certs")
                                                          .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("The following variables");
        exception.expectMessage(DOCKER_HOST);
        exception.expectMessage("cannot exist in your additional environment variable block");

        new LocalBuilder(REMOTE, dockerVariables).withAdditionalEnvironmentVariable(DOCKER_HOST, "tcp://192.168.99.101:2376")
                                                 .build();
    }

    @Test
    public void return_localhost_as_ip_daemon() {
        DockerMachine localMachine = new LocalBuilder(DAEMON, new HashMap<>()).build();
        assertThat(localMachine.getIp(), is(LOCALHOST));
    }

    @Test
    public void return_docker_host_as_ip_remote() {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).build();
        assertThat(localMachine.getIp(), is("192.168.99.100"));
    }

    @Test
    public void have_missing_docker_host_remote() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_HOST);
        new LocalBuilder(REMOTE, new HashMap<>()).build();
    }

    @Test
    public void build_without_tls_remote() {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                                                          .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                                          .build();

        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).build();
        assertThat(localMachine, containsEnvironment(dockerVariables));
    }

    @Test
    public void have_missing_cert_path_remote() {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_CERT_PATH);
        new LocalBuilder(REMOTE, dockerVariables).build();
    }

    @Test
    public void build_with_tls_remote() {
        Map<String, String> dockerVariables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        DockerMachine localMachine = new LocalBuilder(REMOTE, dockerVariables).build();
        assertThat(localMachine, containsEnvironment(dockerVariables));
    }
}
