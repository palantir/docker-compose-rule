package com.palantir.docker.compose.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.jayway.awaitility.Awaitility;
import com.palantir.docker.compose.execution.DockerComposeExecutable;

public class Container {

    private static final Logger log = LoggerFactory.getLogger(Container.class);

    private final String containerName;
    private final DockerComposeExecutable dockerComposeProcess;

    private final Supplier<Ports> portMappings = Suppliers.memoize(this::getDockerPorts);

    public Container(String containerName, DockerComposeExecutable dockerComposeProcess) {
        this.containerName = containerName;
        this.dockerComposeProcess = dockerComposeProcess;
    }

    public String getContainerName() {
        return containerName;
    }

    public boolean waitForPorts(Duration timeout) {
        try {
            Ports exposedPorts = portMappings.get();
            exposedPorts.waitToBeListeningWithin(timeout);
            return true;
        } catch (Exception e) {
            log.warn("Container '" + containerName + "' failed to come up: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean waitForHttpPort(int internalPort, Function<DockerPort, String> urlFunction, Duration timeout) {
        try {
            DockerPort port = portMappedInternallyTo(internalPort);
            Awaitility.await()
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .atMost(timeout.getMillis(), TimeUnit.MILLISECONDS)
                .until(() ->
                               assertThat(port.isListeningNow() && port.isHttpResponding(urlFunction), is(true))
                );
            return true;
        } catch (Exception e) {
            log.warn("Container '" + containerName + "' failed to come up: " + e.getMessage(), e);
            return false;
        }
    }

    public DockerPort portMappedExternallyTo(int externalPort) throws IOException, InterruptedException {
        return portMappings.get()
                           .stream()
                           .filter(port -> port.getExternalPort() == externalPort)
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("No port mapped externally to '" + externalPort + "' for container '" + containerName + "'"));
    }

    public DockerPort portMappedInternallyTo(int internalPort) throws IOException, InterruptedException {
        return portMappings.get()
                           .stream()
                           .filter(port -> port.getInternalPort() == internalPort)
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("No internal port '" + internalPort + "' for container '" + containerName + "'"));
    }

    private Ports getDockerPorts() {
        try {
            return dockerComposeProcess.ports(containerName);
        } catch (IOException | InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Container container = (Container) o;
        return Objects.equals(containerName, container.containerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerName);
    }

    @Override
    public String toString() {
        return "Container{" +
                "containerName='" + containerName + '\'' +
                '}';
    }
}
