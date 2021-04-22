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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdditionalEnvironmentValidator {
    private static final ImmutableSet<String> ILLEGAL_VARIABLES = ImmutableSet.of(
            EnvironmentVariables.DOCKER_TLS_VERIFY,
            EnvironmentVariables.DOCKER_HOST,
            EnvironmentVariables.DOCKER_CERT_PATH);

    private AdditionalEnvironmentValidator() {}

    public static Map<String, String> validate(Map<String, String> additionalEnvironment) {
        Set<String> invalidVariables = Sets.intersection(additionalEnvironment.keySet(), ILLEGAL_VARIABLES);
        String errorMessage = invalidVariables.stream()
                .collect(Collectors.joining(
                        ", ",
                        "The following variables: ",
                        " cannot exist in your additional environment variable block as they will interfere"
                                + " with Docker."));
        Preconditions.checkState(invalidVariables.isEmpty(), errorMessage);
        return additionalEnvironment;
    }
}
