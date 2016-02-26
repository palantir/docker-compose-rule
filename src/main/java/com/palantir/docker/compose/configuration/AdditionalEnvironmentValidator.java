package com.palantir.docker.compose.configuration;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

public class AdditionalEnvironmentValidator {

    private static final Set<String> ILLEGAL_VARIABLES = ImmutableSet.of(DOCKER_TLS_VERIFY, DOCKER_HOST, DOCKER_CERT_PATH);

    private AdditionalEnvironmentValidator() {}

    public static Map<String, String> validate(Map<String, String> additionalEnvironment) {
        Set<String> invalidVariables = Sets.intersection(additionalEnvironment.keySet(), ILLEGAL_VARIABLES);
        String errorMessage = invalidVariables.stream()
                                              .collect(Collectors.joining(", ",
                                                                          "The following variables: ",
                                                                          " cannot exist in your additional environment variable block as they will interfere with Docker."));
        checkState(invalidVariables.isEmpty(), errorMessage);
        return additionalEnvironment;
    }
}
