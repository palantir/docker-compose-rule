package com.palantir.docker.compose.configuration;

import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.DOCKER_TLS_VERIFY;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public enum AdditionalEnvironmentValidator {
    INSTANCE;

    private static final Set<String> ILLEGAL_VARIABLES = ImmutableSet.of(DOCKER_TLS_VERIFY, DOCKER_HOST, DOCKER_CERT_PATH);

    public Map<String, String> validate(Map<String, String> additionalEnvironment) {
        Set<String> invalidVariables = Sets.intersection(additionalEnvironment.keySet(), ILLEGAL_VARIABLES);

        if (invalidVariables.isEmpty()) {
            return additionalEnvironment;
        }

        String errorMessage = invalidVariables.stream()
                                              .collect(Collectors.joining(", ",
                                                                          "The following variables: ",
                                                                          " cannot exist in your additional environment variable block as they will interfere with Docker."));
        throw new IllegalArgumentException(errorMessage);
    }
}
