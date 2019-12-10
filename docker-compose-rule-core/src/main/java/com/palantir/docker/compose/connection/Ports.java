/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Ports {

    // Note: This regex doesn't account for usage of the
    // udp protocol or port ranges (ex: 9090-9091:8080-8081)
    // Without escape characters: (?<ipAddress>(\d+).(\d+).(\d+).(\d+)):(?<externalPort>\d+)->(?<internalPort>\d+)\/tcp
    private static final Pattern PORT_PATTERN = Pattern.compile(
            "(?<ipAddress>(\\d+).(\\d+).(\\d+).(\\d+)):(?<externalPort>\\d+)->(?<internalPort>\\d+)\\/tcp");

    private static final String IP_ADDRESS = "ipAddress";
    private static final String EXTERNAL_PORT = "externalPort";
    private static final String INTERNAL_PORT = "internalPort";

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

    public static Ports parseFromPortInformation(String portInformation, String dockerMachineIp) {
        Matcher matcher = PORT_PATTERN.matcher(portInformation);
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

    @Override
    public int hashCode() {
        return Objects.hash(ports);
    }

    @Override
    @SuppressWarnings("EqualsGetClass")
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
