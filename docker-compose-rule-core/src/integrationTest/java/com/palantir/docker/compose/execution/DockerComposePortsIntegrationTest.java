/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.docker.compose.execution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.palantir.docker.compose.DockerComposeManager;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.Ports;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DockerComposePortsIntegrationTest {

    private static final String LOCALHOST_IP = "127.0.0.1";

    private static DockerComposeManager dockerComposeManager = new DockerComposeManager.Builder()
                .shutdownStrategy(ShutdownStrategy.KILL_DOWN)
                .file("src/test/resources/ports-docker-compose.yaml")
                .build();

    @BeforeClass
    public static void beforeClass() throws IOException, InterruptedException {
        dockerComposeManager.before();
    }

    @Test
    public void no_ports_mapped() throws IOException, InterruptedException {
        Ports ports = dockerComposeManager.dockerCompose().ports("no-ports-mapped");
        Ports expectedPorts = new Ports(ImmutableList.of());
        assertThat(ports, is(expectedPorts));
    }

    @Test
    public void ports_mapped_identically() throws IOException, InterruptedException {
        Ports ports = dockerComposeManager.dockerCompose().ports("ports-mapped-identically");
        Ports expectedPorts = new Ports(ImmutableList.of(
                new DockerPort(LOCALHOST_IP, 5432, 5432)));
        assertThat(ports, is(expectedPorts));
    }

    @Test
    public void ports_mapped_differently() throws IOException, InterruptedException {
        Ports ports = dockerComposeManager.dockerCompose().ports("ports-mapped-differently");
        Ports expectedPorts = new Ports(ImmutableList.of(
                new DockerPort(LOCALHOST_IP, 1234, 5678)));
        assertThat(ports, is(expectedPorts));
    }

    @Test
    public void ports_mapped_with_different_ip() throws IOException, InterruptedException {
        Ports ports = dockerComposeManager.dockerCompose().ports("ports-mapped-with-different-ip");
        Ports expectedPorts = new Ports(ImmutableList.of(
                new DockerPort(LOCALHOST_IP, 8000, 8000)));
        assertThat(ports, is(expectedPorts));
    }

    @Test
    public void ports_exposed_but_not_mapped() throws IOException, InterruptedException {
        Ports ports = dockerComposeManager.dockerCompose().ports("ports-exposed-but-not-mapped");
        Ports expectedPorts = new Ports(ImmutableList.of());
        assertThat(ports, is(expectedPorts));
    }

    @Test
    public void lots_of_port_information() throws IOException, InterruptedException {
        Ports ports = dockerComposeManager.dockerCompose().ports("lots-of-port-information");
        Set<DockerPort> expectedPortsSet = ImmutableSet.of(
                new DockerPort(LOCALHOST_IP, 9000, 9000),
                new DockerPort(LOCALHOST_IP, 9010, 9010),
                new DockerPort(LOCALHOST_IP, 9020, 9020),
                new DockerPort(LOCALHOST_IP, 6666, 7777),
                new DockerPort(LOCALHOST_IP, 7777, 8888),
                new DockerPort(LOCALHOST_IP, 8888, 9999),
                new DockerPort(LOCALHOST_IP, 4000, 4000),
                new DockerPort(LOCALHOST_IP, 4010, 4010),
                new DockerPort(LOCALHOST_IP, 4020, 4020));
        assertThat(ports.stream().collect(Collectors.toSet()), is(expectedPortsSet));
    }

    @AfterClass
    public static void afterClass() {
        dockerComposeManager.after();
    }
}
