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
package com.palantir.docker.compose.configuration;

import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DaemonEnvironmentValidatorShould {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void validate_successfully_when_docker_environment_does_not_contain_docker_variables() {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                                                    .put("SOME_VARIABLE", "SOME_VALUE")
                                                    .put("ANOTHER_VARIABLE", "ANOTHER_VALUE")
                                                    .build();

        DaemonEnvironmentValidator.instance().validateEnvironmentVariables(variables);
    }

    @Test
    public void throw_exception_when_docker_environment_contains_illegal_docker_variables() {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put(DOCKER_HOST, "tcp://192.168.99.100:2376")
                .put(DOCKER_TLS_VERIFY, "1")
                .put(DOCKER_CERT_PATH, "/path/to/certs")
                .build();

        exception.expect(IllegalStateException.class);
        exception.expectMessage("These variables were set:");
        exception.expectMessage(DOCKER_HOST);
        exception.expectMessage(DOCKER_CERT_PATH);
        exception.expectMessage(DOCKER_TLS_VERIFY);
        exception.expectMessage("They cannot be set when connecting to a local docker daemon");
        DaemonEnvironmentValidator.instance().validateEnvironmentVariables(variables);
    }

}
