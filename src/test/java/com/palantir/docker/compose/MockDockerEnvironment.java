package com.palantir.docker.compose;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

public class MockDockerEnvironment {

    private final DockerComposeExecutable dockerComposeProcess;
    private final DockerMachine dockerMachine;

    public MockDockerEnvironment(DockerComposeExecutable dockerComposeProcess,
                                 DockerMachine dockerMachine) {
        this.dockerComposeProcess = dockerComposeProcess;
        this.dockerMachine = dockerMachine;
    }

    public DockerPort availableService(String service, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = port(service, externalPortNumber, internalPortNumber);
        when(port.isListeningNow()).thenReturn(true);
        return port;
    }

    public DockerPort availableHttpService(String service, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = availableService(service, externalPortNumber, internalPortNumber);
        when(port.isHttpResponding(any())).thenReturn(true);
        return port;
    }

    public DockerPort unavailableService(String service, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = port(service, externalPortNumber, internalPortNumber);
        when(port.isListeningNow()).thenReturn(false);
        return port;
    }

    public DockerPort port(String service, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = mock(DockerPort.class);
        when(port.getExternalPort()).thenReturn(externalPortNumber);
        when(port.getInternalPort()).thenReturn(internalPortNumber);
        PortMapping portMapping = new PortMapping(externalPortNumber, internalPortNumber);
        when(dockerComposeProcess.ports(service)).thenReturn(new PortMappings(portMapping));
        when(dockerMachine.getPort(portMapping)).thenReturn(port);
        when(dockerMachine.portsFor(new PortMappings(portMapping))).thenReturn(new Ports(port));
        return port;
    }

}
