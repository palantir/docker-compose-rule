package com.palantir.docker.compose.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.jayway.awaitility.Awaitility;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Container {

    private static final Logger log = LoggerFactory.getLogger(Container.class);

    private final String containerName;
    private final DockerComposeExecutable dockerComposeProcess;
    private final DockerMachine dockerMachine;

    private final Supplier<PortMappings> portMappings = Suppliers.memoize(this::getDockerPorts);

    public Container(String containerName,
                    DockerComposeExecutable dockerComposeProcess,
                    DockerMachine dockerMachine) {
        this.containerName = containerName;
        this.dockerComposeProcess = dockerComposeProcess;
        this.dockerMachine = dockerMachine;
    }

    public String getContainerName() {
        return containerName;
    }

    public boolean waitForPorts(Duration timeout) {
        try {
            PortMappings exposedPorts = portMappings.get();
            dockerMachine.portsFor(exposedPorts).waitToBeListeningWithin(timeout);
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
                .until(() -> assertThat(port.isListeningNow() && port.isHttpResponding(urlFunction), is(true)));
            return true;
        } catch (Exception e) {
            log.warn("Container '" + containerName + "' failed to come up: " + e.getMessage(), e);
            return false;
        }
    }

    public DockerPort portMappedExternallyTo(int externalPort) throws IOException, InterruptedException {
        return portMappings.get()
                           .stream()
                           .map(dockerMachine::getPort)
                           .filter(port -> port.getExternalPort() == externalPort)
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("No port mapped externally to '" + externalPort + "' for container '" + containerName + "'"));
    }

    public DockerPort portMappedInternallyTo(int internalPort) throws IOException, InterruptedException {
        return portMappings.get()
                           .stream()
                           .map(dockerMachine::getPort)
                           .filter(port -> port.getInternalPort() == internalPort)
                           .findFirst()
                           .orElseThrow(() -> new IllegalArgumentException("No internal port '" + internalPort + "' for container '" + containerName + "'"));
    }

    private PortMappings getDockerPorts() {
        try {
            return dockerComposeProcess.ports(containerName);
        } catch (IOException | InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerName, dockerComposeProcess, dockerMachine);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Container other = (Container) obj;
        return Objects.equals(containerName, other.containerName) &&
               Objects.equals(dockerComposeProcess, other.dockerComposeProcess) &&
               Objects.equals(dockerMachine, other.dockerMachine);
    }

    @Override
    public String toString() {
        return "Service [serviceName=" + containerName
                + ", dockerComposeProcess=" + dockerComposeProcess
                + ", dockerMachine=" + dockerMachine + "]";
    }

}
