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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PortsShould {

    private static final String LOCALHOST_IP = "127.0.0.1";
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void result_in_no_ports_when_there_are_no_ports_in_port_information() {
        String portInformation = "";
        Ports ports = Ports.parseFromPortInformation(portInformation, LOCALHOST_IP);
        Ports expected = new Ports(emptyList());
        assertThat(ports, is(expected));
    }

    @Test
    public void result_in_single_port_when_there_is_single_tcp_port_mapping() {
        String portInformation = "0.0.0.0:5432->5432/tcp";
        Ports ports = Ports.parseFromPortInformation(portInformation, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 5432, 5432)));
        assertThat(ports, is(expected));
    }

    @Test
    public void result_in_single_port_with_ip_other_than_localhost_when_there_is_single_tcp_port_mapping() {
        String portInformation = "10.0.1.2:1234->2345/tcp";
        Ports ports = Ports.parseFromPortInformation(portInformation, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort("10.0.1.2", 1234, 2345)));
        assertThat(ports, is(expected));
    }

    @Test
    public void result_in_two_ports_when_there_are_two_tcp_port_mappings() {
        String portInformation = "0.0.0.0:5432->5432/tcp, 0.0.0.0:5433->5432/tcp";
        Ports ports = Ports.parseFromPortInformation(portInformation, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 5432, 5432),
                                                new DockerPort(LOCALHOST_IP, 5433, 5432)));
        assertThat(ports, is(expected));
    }

    @Test
    public void result_in_no_ports_when_there_is_a_non_mapped_exposed_port() {
        String portInformation = "5432/tcp";
        Ports ports = Ports.parseFromPortInformation(portInformation, LOCALHOST_IP);
        Ports expected = new Ports(emptyList());
        assertThat(ports, is(expected));
    }

    @Test
    public void result_in_a_variety_of_ports_when_there_are_many_ports_in_port_information() {
        String portInformation = "0.0.0.0:8880->8880/tcp, 0.0.0.0:8881->8881/tcp, 8882/tcp, 8883/tcp, 8884/tcp";
        Ports ports = Ports.parseFromPortInformation(portInformation, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 8880, 8880),
                                                new DockerPort(LOCALHOST_IP, 8881, 8881)));
        assertThat(ports, is(expected));
    }
}
