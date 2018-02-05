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
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Container {

    private final String containerName;
    private final Docker docker;
    private final DockerCompose dockerCompose;

    private final Supplier<Ports> portMappings = Suppliers.memoize(this::getDockerPorts);

    public Container(String containerName, Docker docker, DockerCompose dockerCompose) {
        this.containerName = containerName;
        this.docker = docker;
        this.dockerCompose = dockerCompose;
    }

    public String getContainerName() {
        return containerName;
    }

    public SuccessOrFailure portIsListeningOnHttpAndCheckStatus2xx(int internalPort, Function<DockerPort, String> urlFunction) {
        return portIsListeningOnHttp(internalPort, urlFunction, true);
    }

    public SuccessOrFailure portIsListeningOnHttp(int internalPort, Function<DockerPort, String> urlFunction) {
        return portIsListeningOnHttp(internalPort, urlFunction, false);
    }

    public SuccessOrFailure portIsListeningOnHttp(int internalPort, Function<DockerPort, String> urlFunction, boolean andCheckStatus) {
        try {
            DockerPort port = port(internalPort);
            if (!port.isListeningNow()) {
                return SuccessOrFailure.failure(internalPort + " is not listening");
            }
            return port.isHttpRespondingSuccessfully(urlFunction, andCheckStatus)
                    .mapFailure(failureMessage -> internalPort + " does not have a http response from " + urlFunction.apply(port) + ":\n" + failureMessage);
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

    /**
     * @deprecated Please use `port(internalPort)` instead.
     */
    @Deprecated
    public DockerPort portMappedInternallyTo(int internalPort) {
        return port(internalPort);
    }

    public DockerPort port(int internalPort) {
        return portMappings.get()
                           .stream()
                           .filter(port -> port.getInternalPort() == internalPort)
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("No internal port '" + internalPort + "' for container '" + containerName + "': " + portMappings));
    }

    public void start() throws IOException, InterruptedException {
        dockerCompose.start(this);
    }

    public void stop() throws IOException, InterruptedException {
        dockerCompose.stop(this);
    }

    public void kill() throws IOException, InterruptedException {
        dockerCompose.kill(this);
    }

    public State state() throws IOException, InterruptedException {
        String id = dockerCompose.id(this).orElse(null);
        if (id == null) {
            return State.DOWN;
        }
        return docker.state(id);
    }

    public void up() throws IOException, InterruptedException {
        dockerCompose.up(this);
    }

    public Ports ports() {
        return portMappings.get();
    }

    private Ports getDockerPorts() {
        try {
            return dockerCompose.ports(containerName);
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
        return "Container{containerName='" + containerName + "'}";
    }

    public SuccessOrFailure areAllPortsOpen() {
        List<Integer> unavailablePorts = portMappings.get().stream()
                .filter(port -> !port.isListeningNow())
                .map(DockerPort::getInternalPort)
                .collect(Collectors.toList());

        boolean allPortsOpen = unavailablePorts.isEmpty();
        String failureMessage = "The following ports failed to open: " + unavailablePorts;

        return SuccessOrFailure.fromBoolean(allPortsOpen, failureMessage);
    }


    public SuccessOrFailure doesTextAppearInLogs(String text) {
        try {
            String logs = dockerCompose.readLogs(containerName);
            String failureMessage=String.format("Text '%s' not found in logs of container %s", text, containerName);
            return SuccessOrFailure.fromBoolean(logs.contains(text), failureMessage);
        } catch (Exception e) {
            return SuccessOrFailure.fromException(e);
        }

    }
}
