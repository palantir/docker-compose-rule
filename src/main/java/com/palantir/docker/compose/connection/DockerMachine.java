package com.palantir.docker.compose.connection;

import com.palantir.docker.compose.configuration.DockerEnvironmentVariables;

public class DockerMachine {
    private final String ip;

    public DockerMachine(String ip) {
        this.ip = ip;
    }

    public static DockerMachine fromEnvironment() {
        DockerEnvironmentVariables envVars = new DockerEnvironmentVariables(System.getenv());
        return new DockerMachine(envVars.getDockerHostIp());
    }

    public String getIp() {
        return ip;
    }

}
