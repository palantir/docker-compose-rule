/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.docker.compose.configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.Ports;
import com.palantir.docker.compose.execution.DockerCompose;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MockDockerEnvironment {

    private final DockerCompose dockerComposeProcess;

    public MockDockerEnvironment(DockerCompose dockerComposeProcess) {
        this.dockerComposeProcess = dockerComposeProcess;
    }

    public DockerPort availableService(String service, String ip, int externalPortNumber, int internalPortNumber) throws Exception {
        DockerPort port = port(service, ip, externalPortNumber, internalPortNumber);
        doReturn(true).when(port).isListeningNow();
        return port;
    }

    public DockerPort unavailableService(String service, String ip, int externalPortNumber, int internalPortNumber) throws Exception {
        DockerPort port = port(service, ip, externalPortNumber, internalPortNumber);
        doReturn(false).when(port).isListeningNow();
        return port;
    }

    public DockerPort availableHttpService(String service, String ip, int externalPortNumber, int internalPortNumber) throws Exception {
        DockerPort port = availableService(service, ip, externalPortNumber, internalPortNumber);
        doReturn(true).when(port).isHttpResponding(any(), eq(false));
        return port;
    }

    public DockerPort unavailableHttpService(String service, String ip, int externalPortNumber, int internalPortNumber) throws Exception {
        DockerPort port = availableService(service, ip, externalPortNumber, internalPortNumber);
        doReturn(false).when(port).isHttpResponding(any(), eq(false));
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

    private static DockerPort dockerPortSpy(String ip, int externalPortNumber, int internalPortNumber) {
        DockerPort port = new DockerPort(ip, externalPortNumber, internalPortNumber);
        return spy(port);
    }
}
