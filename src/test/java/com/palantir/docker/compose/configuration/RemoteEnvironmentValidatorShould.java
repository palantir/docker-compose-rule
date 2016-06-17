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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RemoteEnvironmentValidatorShould {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void throw_exception_if_docker_host_is_not_set() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                                                    .put("SOME_VARIABLE", "SOME_VALUE")
                                                    .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_HOST);
        RemoteEnvironmentValidator.instance().validateEnvironmentVariables(variables);
    }

    @Test
    public void throw_exception_if_docker_cert_path_is_missing_and_tls_is_on() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("Missing required environment variables: ");
        exception.expectMessage(DOCKER_CERT_PATH);
        RemoteEnvironmentValidator.instance().validateEnvironmentVariables(variables);
    }

    @Test
    public void validate_environment_with_all_valid_variables_set_without_tls() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                                                    .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                                                    .put("SOME_VARIABLE", "SOME_VALUE")
                                                    .build();

        RemoteEnvironmentValidator.instance().validateEnvironmentVariables(variables);
    }

    @Test
    public void validate_environment_with_all_valid_variables_set_with_tls() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .put("SOME_VARIABLE", "SOME_VALUE")
                .build();

        RemoteEnvironmentValidator.instance().validateEnvironmentVariables(variables);
    }
}
