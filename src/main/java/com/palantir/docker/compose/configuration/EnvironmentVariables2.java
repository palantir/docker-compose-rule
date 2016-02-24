package com.palantir.docker.compose.configuration;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

public class EnvironmentVariables2 {

    public static final String OS_NAME = "os.name";
    public static final String MAC_OS = "Mac";
    public static final String TCP_PROTOCOL = "tcp://";
    public static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";
    public static final String DOCKER_HOST = "DOCKER_HOST";
    public static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";

    private final String hostIp;
    private final Map<String, String> environment;

    public EnvironmentVariables2(EnvironmentValidator validator,
                                 HostIpResolver ipResolver,
                                 Map<String, String> dockerEnvironment,
                                 Map<String, String> additionalEnvironment) {
        this.environment = ImmutableMap.<String, String>builder()
                .putAll(validator.validate(dockerEnvironment))
                .putAll(EnvironmentValidator.ADDITIONAL.validate(additionalEnvironment))
                .build();
        this.hostIp = ipResolver.resolveIp(dockerEnvironment.getOrDefault(DOCKER_HOST, ""));
    }

    public String getHostIp() {
        return hostIp;
    }

    public void augmentGivenEnvironment(Map<String, String> environmentToAugment) {
        environmentToAugment.putAll(environment);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnvironmentVariables2 that = (EnvironmentVariables2) o;
        return Objects.equals(environment, that.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environment);
    }

    @Override
    public String toString() {
        return "EnvironmentVariables{" +
                "environment=" + environment +
                '}';
    }
}
