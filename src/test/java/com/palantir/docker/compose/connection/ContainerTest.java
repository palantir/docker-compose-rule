/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * THIS SOFTWARE CONTAINS PROPRIETARY AND CONFIDENTIAL INFORMATION OWNED BY PALANTIR TECHNOLOGIES INC.
 * UNAUTHORIZED DISCLOSURE TO ANY THIRD PARTY IS STRICTLY PROHIBITED
 *
 * For good and valuable consideration, the receipt and adequacy of which is acknowledged by Palantir and recipient
 * of this file ("Recipient"), the parties agree as follows:
 *
 * This file is being provided subject to the non-disclosure terms by and between Palantir and the Recipient.
 *
 * Palantir solely shall own and hereby retains all rights, title and interest in and to this software (including,
 * without limitation, all patent, copyright, trademark, trade secret and other intellectual property rights) and
 * all copies, modifications and derivative works thereof.  Recipient shall and hereby does irrevocably transfer and
 * assign to Palantir all right, title and interest it may have in the foregoing to Palantir and Palantir hereby
 * accepts such transfer. In using this software, Recipient acknowledges that no ownership rights are being conveyed
 * to Recipient.  This software shall only be used in conjunction with properly licensed Palantir products or
 * services.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.palantir.docker.compose.connection;

import com.palantir.docker.compose.configuration.MockDockerEnvironment;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ContainerTest {

    private static final String IP = "127.0.0.1";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerComposeExecutable dockerComposeProcess = mock(DockerComposeExecutable.class);
    private final MockDockerEnvironment env = new MockDockerEnvironment(dockerComposeProcess);
    private final Container service = new Container("service", dockerComposeProcess);

    @Test
    public void waiting_for_a_containers_ports_waits_for_the_ports_from_docker_compose_ps_to_be_available() throws IOException, InterruptedException {
        DockerPort port = env.availableService("service", IP, 5433, 5432);
        assertThat(service.waitForPorts(Duration.millis(100)), is(true));
        verify(port, atLeastOnce()).isListeningNow();
    }

    @Test
    public void wait_for_a_containers_ports_returns_false_when_the_port_is_unavailable() throws IOException, InterruptedException {
        env.unavailableService("service", IP, 5433, 5432);
        assertThat(service.waitForPorts(Duration.millis(100)), is(false));
    }

    @Test
    public void waiting_for_a_containers_http_ports_waits_for_the_ports_from_docker_compose_ps_to_be_available() throws IOException, InterruptedException {
        DockerPort port = env.availableHttpService("service", IP, 5433, 5432);
        assertThat(service.waitForHttpPort(5432, (p) -> "url", Duration.millis(100)), is(true));
        verify(port, atLeastOnce()).isListeningNow();
    }

    @Test
    public void wait_for_a_containers_http_ports_returns_false_when_the_port_is_unavailable() throws IOException, InterruptedException {
        env.unavailableService("service", IP, 5433, 5432);
        assertThat(service.waitForHttpPort(5432, (p) -> "url", Duration.millis(100)), is(false));
    }

    @Test
    public void port_is_returned_for_container_when_external_port_number_given() throws IOException, InterruptedException {
        DockerPort expected = env.availableService("service", IP, 5433, 5432);
        DockerPort port = service.portMappedExternallyTo(5433);
        assertThat(port, is(expected));
    }

    @Test
    public void port_is_returned_for_container_when_internal_port_number_given() throws IOException, InterruptedException {
        DockerPort expected = env.availableService("service", IP, 5433, 5432);
        DockerPort port = service.portMappedInternallyTo(5432);
        assertThat(port, is(expected));
    }

    @Test
    public void when_two_ports_are_requested_docker_ports_is_only_called_once() throws IOException, InterruptedException {
        env.ports("service", IP, 8080, 8081);
        service.portMappedInternallyTo(8080);
        service.portMappedInternallyTo(8081);
        verify(dockerComposeProcess, times(1)).ports("service");
    }

    @Test
    public void requested_a_port_for_an_unknown_external_port_results_in_an_illegal_argument_exception() throws IOException, InterruptedException {
        env.availableService("service", IP, 5400, 5400); // Service must have ports otherwise we end up with an exception telling you the service is listening at all
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No port mapped externally to '5432' for container 'service'");
        service.portMappedExternallyTo(5432);
    }

    @Test
    public void requested_a_port_for_an_unknown_internal_port_results_in_an_illegal_argument_exception() throws IOException, InterruptedException {
        env.availableService("service", IP, 5400, 5400);
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No internal port '5432' for container 'service'");
        service.portMappedInternallyTo(5432);
    }

}
