package com.palantir.docker.compose.configuration;

import static com.google.common.collect.Sets.newHashSet;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_CERT_PATH;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_HOST;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.DOCKER_TLS_VERIFY;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class RemoteEnvironmentValidator {

    private static final Set<String> SECURE_VARIABLES = ImmutableSet.of(DOCKER_TLS_VERIFY, DOCKER_CERT_PATH);

    private final Map<String, String> dockerEnvironment;

    public RemoteEnvironmentValidator(Map<String, String> dockerEnvironment) {
        this.dockerEnvironment = dockerEnvironment;
    }

    public Map<String, String> validate() {
        Collection<String> missingEnvironmentVariables = getMissingEnvVariables();

        if (missingEnvironmentVariables.isEmpty()) {
            return dockerEnvironment;
        }

        throw new IllegalStateException("Missing required environment variables: '" + missingEnvironmentVariables
                                                + "', please run `docker-machine env <machine-name>` and ensure they are set on the DockerComposition.");
    }

    private Collection<String> getMissingEnvVariables() {
        Collection<String> requiredVariables = Sets.union(newHashSet(DOCKER_HOST), secureVariablesRequired());
        return requiredVariables.stream()
                                .filter(envVariable -> Strings.isNullOrEmpty(dockerEnvironment.get(envVariable)))
                                .collect(Collectors.toSet());
    }

    private Set<String> secureVariablesRequired() {
        return certVerificationEnabled() ? SECURE_VARIABLES : newHashSet();
    }

    private boolean certVerificationEnabled() {
        return dockerEnvironment.containsKey(DOCKER_TLS_VERIFY);
    }

}
