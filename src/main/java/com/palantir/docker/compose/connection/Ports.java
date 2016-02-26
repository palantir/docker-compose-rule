package com.palantir.docker.compose.connection;

import static java.util.stream.Collectors.toList;

import static com.palantir.docker.compose.matchers.AvailablePortMatcher.areAvailable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;

public class Ports {

    private static final Logger log = LoggerFactory.getLogger(Ports.class);
    private static final Pattern PORT_PATTERN = Pattern.compile("((\\d+).(\\d+).(\\d+).(\\d+)):(\\d+)->(\\d+)/tcp");
    private static final int IP_ADDRESS = 1;
    private static final int EXTERNAL_PORT = 6;
    private static final int INTERNAL_PORT = 7;

    private static final String NO_IP_ADDRESS = "0.0.0.0";

    private final List<DockerPort> ports;

    public Ports(List<DockerPort> ports) {
        this.ports = ports;
    }

    public Ports(DockerPort port) {
        this(Collections.singletonList(port));
    }

    public Stream<DockerPort> stream() {
        return ports.stream();
    }

    public static Ports parseFromDockerComposePs(String psOutput, String dockerMachineIp) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(psOutput), "No container found");
        Matcher matcher = PORT_PATTERN.matcher(psOutput);
        List<DockerPort> ports = new ArrayList<>();
        while (matcher.find()) {
            String matchedIpAddress = matcher.group(IP_ADDRESS);
            String ip = matchedIpAddress.equals(NO_IP_ADDRESS) ? dockerMachineIp : matchedIpAddress;
            int externalPort = Integer.parseInt(matcher.group(EXTERNAL_PORT));
            int internalPort = Integer.parseInt(matcher.group(INTERNAL_PORT));

            ports.add(new DockerPort(ip, externalPort, internalPort));
        }
        return new Ports(ports);
    }

    public void waitToBeListeningWithin(Duration timeout) throws InterruptedException {
        try {
            waitForPorts(timeout);
        } catch (ConditionTimeoutException e) {
            throw new IllegalStateException(e);
        }
    }

    private void waitForPorts(Duration timeout) {
        log.info("Waiting {} for ports '{}' to be open", timeout, ports);
        Awaitility.await()
                  .pollInterval(50, TimeUnit.MILLISECONDS)
                  .atMost(timeout.getMillis(), TimeUnit.MILLISECONDS)
                  .until(this::unavailablePorts, areAvailable());
        log.info("All ports open");
    }

    private Collection<DockerPort> unavailablePorts() {
        return ports.stream()
                    .filter(port -> !port.isListeningNow())
                    .collect(toList());
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
