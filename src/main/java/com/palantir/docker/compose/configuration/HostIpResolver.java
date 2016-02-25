package com.palantir.docker.compose.configuration;

import static com.google.common.base.Strings.emptyToNull;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.TCP_PROTOCOL;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public enum HostIpResolver {

    DAEMON {
        @Override
        public String resolveIp(String dockerHost) {
            return LOCALHOST;
        }
    },

    REMOTE {
        @Override
        public String resolveIp(String dockerHost) {
            return Optional.ofNullable(emptyToNull(dockerHost))
                           .map(host -> StringUtils.substringAfter(host, TCP_PROTOCOL))
                           .map(ipAndMaybePort -> StringUtils.substringBefore(ipAndMaybePort, ":"))
                           .orElseThrow(() -> new IllegalArgumentException("DOCKER_HOST cannot be blank/null"));
        }
    };

    public static final String LOCALHOST = "127.0.0.1";

    public abstract String resolveIp(String dockerHost);

}
