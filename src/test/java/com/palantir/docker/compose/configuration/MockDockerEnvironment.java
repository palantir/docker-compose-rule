package com.palantir.docker.compose.configuration;

import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.Ports;
import com.palantir.docker.compose.execution.DockerComposeExecutable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MockDockerEnvironment {

    private final DockerComposeExecutable dockerComposeProcess;

    public MockDockerEnvironment(DockerComposeExecutable dockerComposeProcess) {
        this.dockerComposeProcess = dockerComposeProcess;
    }

    public DockerPort availableService(String service, String ip, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = port(service, ip, externalPortNumber, internalPortNumber);
        doReturn(true).when(port).isListeningNow();
        return port;
    }

    public DockerPort availableHttpService(String service, String ip, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = availableService(service, ip, externalPortNumber, internalPortNumber);
        doReturn(true).when(port).isHttpResponding(any());
        return port;
    }

    public DockerPort unavailableService(String service, String ip, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = port(service, ip, externalPortNumber, internalPortNumber);
        doReturn(false).when(port).isListeningNow();
        return port;
    }

    public DockerPort port(String service, String ip, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = dockerPortSpy(ip, externalPortNumber, internalPortNumber);
        when(dockerComposeProcess.ports(service)).thenReturn(new Ports(port));
        return port;
    }

    public void ports(String service, String ip, Integer... portNumbers) throws IOException, InterruptedException {
        List<DockerPort> ports = Arrays.asList(portNumbers)
                                         .stream()
                                         .map(portNumber -> dockerPortSpy(ip, portNumber, portNumber))
                                         .collect(Collectors.toList());
        when(dockerComposeProcess.ports(service)).thenReturn(new Ports(ports));
    }

    private DockerPort dockerPortSpy(String ip, int externalPortNumber, int internalPortNumber) {
        DockerPort port = new DockerPort(ip, externalPortNumber, internalPortNumber);
        return spy(port);
    }

}
