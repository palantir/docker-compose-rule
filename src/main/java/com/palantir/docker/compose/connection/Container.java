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
package com.palantir.docker.compose.connection;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import com.palantir.docker.compose.execution.DockerCompose;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Container {

    private final String containerName;
    private final DockerCompose dockerComposeProcess;

    private final Supplier<Ports> portMappings = Suppliers.memoize(this::getDockerPorts);

    public Container(String containerName, DockerCompose dockerComposeProcess) {
        this.containerName = containerName;
        this.dockerComposeProcess = dockerComposeProcess;
    }

    public String getContainerName() {
        return containerName;
    }

    public SuccessOrFailure portIsListeningOnHttp(int internalPort, Function<DockerPort, String> urlFunction) {
        try {
            DockerPort port = portMappedInternallyTo(internalPort);
            if (!port.isListeningNow()) {
                return SuccessOrFailure.failure(internalPort + " is not listening");
            }
            if (!port.isHttpResponding(urlFunction)) {
                return SuccessOrFailure.failure(internalPort + " does not have a http response from " + urlFunction.apply(port));
            }
            return SuccessOrFailure.success();
        } catch (Exception e) {
            return SuccessOrFailure.fromException(e);
        }
    }

    public DockerPort portMappedExternallyTo(int externalPort) {
        return portMappings.get()
                           .stream()
                           .filter(port -> port.getExternalPort() == externalPort)
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("No port mapped externally to '" + externalPort + "' for container '" + containerName + "'"));
    }

    public DockerPort portMappedInternallyTo(int internalPort) {
        return portMappings.get()
                           .stream()
                           .filter(port -> port.getInternalPort() == internalPort)
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("No internal port '" + internalPort + "' for container '" + containerName + "'"));
    }

    public void start() throws IOException, InterruptedException {
        dockerComposeProcess.start(this);
    }

    public void stop() throws IOException, InterruptedException {
        dockerComposeProcess.stop(this);
    }

    public State state() throws IOException, InterruptedException {
        return dockerComposeProcess.state(containerName);
    }

    private Ports getDockerPorts() {
        try {
            return dockerComposeProcess.ports(containerName);
        } catch (IOException | InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Container container = (Container) object;
        return Objects.equals(containerName, container.containerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerName);
    }

    @Override
    public String toString() {
        return "Container{containerName='" + containerName + "}";
    }

    public SuccessOrFailure areAllPortsOpen() {
        List<Integer> unavaliablePorts = portMappings.get().stream()
                .filter(port -> !port.isListeningNow())
                .map(DockerPort::getInternalPort)
                .collect(Collectors.toList());

        boolean allPortsOpen = unavaliablePorts.isEmpty();
        String failureMessage = "The following ports failed to open: " + unavaliablePorts;

        return SuccessOrFailure.fromBoolean(allPortsOpen, failureMessage);
    }
}
