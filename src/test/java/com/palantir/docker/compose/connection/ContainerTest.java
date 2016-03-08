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

import com.palantir.docker.compose.configuration.MockDockerEnvironment;
import com.palantir.docker.compose.execution.DockerCompose;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ContainerTest {

    private static final String IP = "127.0.0.1";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerCompose dockerComposeProcess = mock(DockerCompose.class);
    private final MockDockerEnvironment env = new MockDockerEnvironment(dockerComposeProcess);
    private final Container container = new Container("service", dockerComposeProcess);

    @Test
    public void port_is_returned_for_container_when_external_port_number_given() throws IOException, InterruptedException {
        DockerPort expected = env.availableService("service", IP, 5433, 5432);
        DockerPort port = container.portMappedExternallyTo(5433);
        assertThat(port, is(expected));
    }

    @Test
    public void port_is_returned_for_container_when_internal_port_number_given() throws IOException, InterruptedException {
        DockerPort expected = env.availableService("service", IP, 5433, 5432);
        DockerPort port = container.portMappedInternallyTo(5432);
        assertThat(port, is(expected));
    }

    @Test
    public void when_two_ports_are_requested_docker_ports_is_only_called_once() throws IOException, InterruptedException {
        env.ports("service", IP, 8080, 8081);
        container.portMappedInternallyTo(8080);
        container.portMappedInternallyTo(8081);
        verify(dockerComposeProcess, times(1)).ports("service");
    }

    @Test
    public void requested_a_port_for_an_unknown_external_port_results_in_an_illegal_argument_exception() throws IOException, InterruptedException {
        env.availableService("service", IP, 5400, 5400); // Service must have ports otherwise we end up with an exception telling you the service is listening at all
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No port mapped externally to '5432' for container 'service'");
        container.portMappedExternallyTo(5432);
    }

    @Test
    public void requested_a_port_for_an_unknown_internal_port_results_in_an_illegal_argument_exception() throws IOException, InterruptedException {
        env.availableService("service", IP, 5400, 5400);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No internal port '5432' for container 'service'");
        container.portMappedInternallyTo(5432);
    }

    @Test
    public void have_all_ports_open_if_all_exposed_ports_are_open() throws IOException, InterruptedException {
        env.availableHttpService("service", IP, 1234, 1234);

        assertThat(container.areAllPortsOpen(), is(true));
    }

    @Test
    public void not_have_all_ports_open_if_has_at_least_one_closed_port() throws IOException, InterruptedException {
        env.availableService("service", IP, 1234, 1234);
        env.unavailableService("service", IP, 4321, 4321);

        assertThat(container.areAllPortsOpen(), is(false));
    }

    @Test
    public void be_listening_on_http_when_the_port_is() throws IOException, InterruptedException {
        env.availableHttpService("service", IP, 1234, 2345);

        assertThat(
                container.portIsListeningOnHttp(2345, port -> "http://some.url:" + port),
                is(true));
    }

    @Test
    public void not_be_listening_on_http_when_the_port_is_not() throws IOException, InterruptedException {
        env.unavailableHttpService("service", IP, 1234, 1234);

        assertThat(
                container.portIsListeningOnHttp(2345, port -> "http://some.url:" + port),
                is(false));
    }

}
