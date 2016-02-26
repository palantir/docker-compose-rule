package com.palantir.docker.compose.configuration;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static com.google.common.base.Strings.emptyToNull;
import static com.palantir.docker.compose.configuration.EnvironmentVariables.TCP_PROTOCOL;

public class RemoteHostIpResolver implements HostIpResolver {

    @Override
    public String resolveIp(String dockerHost) {
        return Optional.ofNullable(emptyToNull(dockerHost))
                       .map(host -> StringUtils.substringAfter(host, TCP_PROTOCOL))
                       .map(ipAndMaybePort -> StringUtils.substringBefore(ipAndMaybePort, ":"))
                       .orElseThrow(() -> new IllegalArgumentException("DOCKER_HOST cannot be blank/null"));
    }
}
