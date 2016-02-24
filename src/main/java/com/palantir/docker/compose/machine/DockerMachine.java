package com.palantir.docker.compose.machine;

import com.palantir.docker.compose.execution.DockerEnvironmentVariables;
import com.palantir.docker.compose.machine.ports.DockerPort;
import com.palantir.docker.compose.machine.ports.PortMapping;
import com.palantir.docker.compose.machine.ports.PortMappings;
import com.palantir.docker.compose.machine.ports.Ports;

public class DockerMachine {
    private final String ip;

    public DockerMachine(String ip) {
        this.ip = ip;
    }

    public static DockerMachine from(DockerEnvironmentVariables env) {
        return new DockerMachine(env.getDockerHostIp());
    }

    public String getIp() {
        return ip;
    }

    public DockerPort getPort(PortMapping portMapping) {
        return new DockerPort(ip, portMapping.getExternalPort(), portMapping.getInternalPort());
    }

    public Ports portsFor(PortMappings exposedPorts) {
        return new Ports(exposedPorts.stream().map(this::getPort).iterator());
    }

}
