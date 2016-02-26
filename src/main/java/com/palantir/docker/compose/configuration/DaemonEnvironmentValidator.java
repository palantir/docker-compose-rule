package com.palantir.docker.compose.configuration;

import static java.util.stream.Collectors.joining;

import static com.google.common.base.Preconditions.checkState;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

public class DaemonEnvironmentValidator {

    private static final Set<String> ILLEGAL_VARIABLES = ImmutableSet.of(DOCKER_TLS_VERIFY, DOCKER_HOST, DOCKER_CERT_PATH);

    private DaemonEnvironmentValidator() {}

    public static Map<String, String> validate(Map<String, String> dockerEnvironment) {
        Set<String> invalidVariables = ILLEGAL_VARIABLES.stream()
                                                         .filter(dockerEnvironment::containsKey)
                                                         .collect(Collectors.toSet());

        String errorMessage = invalidVariables.stream()
                                              .collect(joining(", ",
                                                               "These variables were set: ",
                                                               ". They cannot be set when connecting to a local docker daemon."));
        checkState(invalidVariables.isEmpty(), errorMessage);
        return dockerEnvironment;
    }

}