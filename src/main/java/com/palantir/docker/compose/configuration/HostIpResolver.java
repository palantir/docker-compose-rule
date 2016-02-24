package com.palantir.docker.compose.configuration;

import static com.google.common.base.Strings.emptyToNull;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.TCP_PROTOCOL;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.MAC_OS;
import static com.palantir.docker.compose.configuration.EnvironmentVariables2.OS_NAME;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public enum HostIpResolver {

    DAEMON {
        @Override
        String resolveIp(String dockerHost) {
            return "127.0.0.1";
        }
    },

    REMOTE {
        @Override
        String resolveIp(String dockerHost) {
            return Optional.ofNullable(emptyToNull(dockerHost))
                           .map(host -> StringUtils.substringAfter(host, TCP_PROTOCOL))
                           .map(ipAndMaybePort -> StringUtils.substringBefore(ipAndMaybePort, ":"))
                           .orElseThrow(() -> new IllegalArgumentException("DOCKER_HOST cannot be blank/null"));
        }
    };

    abstract String resolveIp(String dockerHost);

    public static HostIpResolver getLocalIpResolver(Map<String, String> systemEnvironment) {
        if (systemEnvironment.getOrDefault(OS_NAME, "").startsWith(MAC_OS)) {
            return REMOTE;
        } else {
            return DAEMON;
        }
    }
}
