package com.palantir.docker.compose;

import static java.util.stream.Collectors.toList;

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

    public DockerPort getPort(PortMapping portMapping) {
        return new DockerPort(ip, portMapping.getExternalPort(), portMapping.getInternalPort());
    }

    public Ports portsFor(PortMappings exposedPorts) {
        return new Ports(exposedPorts.stream().map(this::getPort).collect(toList()));
    }

}
