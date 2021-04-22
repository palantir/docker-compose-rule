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

import static com.google.common.base.Preconditions.checkState;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class DaemonEnvironmentValidator implements EnvironmentValidator {

    private static final Set<String> ILLEGAL_VARIABLES = ImmutableSet.of(DOCKER_TLS_VERIFY, DOCKER_HOST, DOCKER_CERT_PATH);
    private static final Supplier<DaemonEnvironmentValidator> SUPPLIER = Suppliers.memoize(
            () -> new DaemonEnvironmentValidator());

    public static DaemonEnvironmentValidator instance() {
        return SUPPLIER.get();
    }

    private DaemonEnvironmentValidator() {}

    @Override
    public void validateEnvironmentVariables(Map<String, String> dockerEnvironment) {
        Set<String> invalidVariables = ILLEGAL_VARIABLES.stream()
                                                         .filter(dockerEnvironment::containsKey)
                                                         .collect(Collectors.toSet());

        String errorMessage = invalidVariables.stream()
                                              .collect(joining(", ",
                                                               "These variables were set: ",
                                                               ". They cannot be set when connecting to a local docker daemon."));
        checkState(invalidVariables.isEmpty(), errorMessage);
    }

}
