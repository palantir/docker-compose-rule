package com.palantir.docker.compose.connection;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.joining;

public class Ports {

    private static final Logger log = LoggerFactory.getLogger(Ports.class);

    private final List<DockerPort> ports;

    public Ports(List<DockerPort> ports) {
        this.ports = ports;
    }

    public Ports(DockerPort port) {
        this(Collections.singletonList(port));
    }

    public void waitToBeListeningWithin(Duration timeout) throws InterruptedException {
        try {
            waitForPorts(timeout);
        } catch (ConditionTimeoutException e) {
            throw new IllegalStateException(buildClosedPortsErrorMessage(), e);
        }
    }

    private void waitForPorts(Duration timeout) {
        log.info("Waiting {} for ports '{}' to be open", timeout, ports);
        Awaitility.await()
                  .pollInterval(50, TimeUnit.MILLISECONDS)
                  .atMost(timeout.getMillis(), TimeUnit.MILLISECONDS)
                  .until(this::checkAllPortsAvailable);
        log.info("All ports open");
    }

    private String buildClosedPortsErrorMessage() {
        return ports.stream()
                    .filter(port -> !port.isListeningNow())
                    .map(port -> "Internal port '" + port.getInternalPort() + "' mapped to '" + port.getExternalPort() + "' was unavailable")
                    .collect(joining("\n"));
    }

    private void checkAllPortsAvailable() {
        if (!ports.stream().allMatch(DockerPort::isListeningNow)) {
            throw new AssertionError(buildClosedPortsErrorMessage());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(ports);
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
        Ports other = (Ports) obj;
        return Objects.equals(ports, other.ports);
    }

    @Override
    public String toString() {
        return "Ports [ports=" + ports + "]";
    }

}
