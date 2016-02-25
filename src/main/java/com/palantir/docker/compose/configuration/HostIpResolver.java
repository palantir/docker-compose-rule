package com.palantir.docker.compose.configuration;

public interface HostIpResolver {

    String resolveIp(String dockerHost);

}
