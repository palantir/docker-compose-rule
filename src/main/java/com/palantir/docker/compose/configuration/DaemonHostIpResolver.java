package com.palantir.docker.compose.configuration;

public class DaemonHostIpResolver implements HostIpResolver {

    public static final String LOCALHOST = "127.0.0.1";

    @Override
    public String resolveIp(String dockerHost) {
        return LOCALHOST;
    }
}
