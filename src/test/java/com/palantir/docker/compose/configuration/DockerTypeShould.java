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
package com.palantir.docker.compose.configuration;

import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

public class DockerTypeShould {

    @Test
    public void return_remote_as_first_valid_type_if_environment_is_illegal_for_daemon() {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();
        assertThat(DockerType.getFirstValidDockerTypeForEnvironment(variables), is(Optional.of(DockerType.REMOTE)));
    }

    @Test
    public void return_daemon_as_first_valid_type_if_environment_is_illegal_for_remote() {
        Map<String, String> variables = ImmutableMap.of();
        assertThat(DockerType.getFirstValidDockerTypeForEnvironment(variables), is(Optional.of(DockerType.DAEMON)));
    }

    @Test
    public void return_absent_as_first_valid_type_if_environment_is_illegal_for_all() {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put(DOCKER_TLS_VERIFY, "1")
                .build();
        assertThat(DockerType.getFirstValidDockerTypeForEnvironment(variables), is(Optional.empty()));
    }

}
