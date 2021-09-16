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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AdditionalEnvironmentValidatorShould {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void throw_exception_when_additional_environment_variables_contain_docker_variables() {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put("DOCKER_HOST", "tcp://some-host:2376")
                .put("SOME_VARIABLE", "Some Value")
                .build();
        exception.expect(IllegalStateException.class);
        exception.expectMessage("The following variables");
        exception.expectMessage("DOCKER_HOST");
        exception.expectMessage("cannot exist in your additional environment");
        AdditionalEnvironmentValidator.validate(variables);
    }

    @Test
    public void validate_arbitrary_environment_variables() {
        Map<String, String> variables = ImmutableMap.<String, String>builder()
                .put("SOME_VARIABLE", "Some Value")
                .build();

        assertThat(AdditionalEnvironmentValidator.validate(variables)).isEqualTo(variables);
    }
}
