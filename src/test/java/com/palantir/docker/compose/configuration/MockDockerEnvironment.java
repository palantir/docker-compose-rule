package com.palantir.docker.compose.configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.PortMapping;
import com.palantir.docker.compose.connection.PortMappings;
import com.palantir.docker.compose.connection.Ports;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        DockerPort port = mockDockerPort(externalPortNumber, internalPortNumber);
        PortMapping portMapping = new PortMapping(externalPortNumber, internalPortNumber);
        when(dockerComposeProcess.ports(service)).thenReturn(new PortMappings(portMapping));
        when(dockerMachine.portsFor(new PortMappings(portMapping))).thenReturn(new Ports(port));
        return port;
    }

    public void ports(String service, int... portNumbers) throws IOException, InterruptedException {
        List<DockerPort> ports = new ArrayList<>();
        List<PortMapping> portMappings = new ArrayList<>();
        for (int portNumber : portNumbers) {
            DockerPort port = mockDockerPort(portNumber, portNumber);
            ports.add(port);
            portMappings.add(new PortMapping(portNumber, portNumber));
        }
        when(dockerComposeProcess.ports(service)).thenReturn(new PortMappings(portMappings));
        when(dockerMachine.portsFor(new PortMappings(portMappings))).thenReturn(new Ports(ports));
    }

    private DockerPort mockDockerPort(int externalPortNumber, int internalPortNumber) {
        DockerPort port = mock(DockerPort.class);
        when(port.getExternalPort()).thenReturn(externalPortNumber);
        when(port.getInternalPort()).thenReturn(internalPortNumber);
        when(dockerMachine.getPort(new PortMapping(externalPortNumber, internalPortNumber))).thenReturn(port);
        return port;
    }

}
