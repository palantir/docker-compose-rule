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

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PortsTest {

    public static final String LOCALHOST_IP = "127.0.0.1";
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerPort port = mock(DockerPort.class);

    @Before
    public void setup() {
        when(port.getInternalPort()).thenReturn(7001);
        when(port.getExternalPort()).thenReturn(7000);
    }

    @Test
    public void no_ports_in_ps_output_results_in_no_ports() throws IOException, InterruptedException {
        String psOutput = "------";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, null);
        Ports expected = new Ports(emptyList());
        assertThat(ports, is(expected));
    }

    @Test
    public void single_tcp_port_mapping_results_in_single_port() throws IOException, InterruptedException {
        String psOutput = "0.0.0.0:5432->5432/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 5432, 5432)));
        assertThat(ports, is(expected));
    }

    @Test
    public void single_tcp_port_mapping_results_in_single_port_with_ip_other_than_localhost() throws IOException, InterruptedException {
        String psOutput = "10.0.1.2:1234->2345/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort("10.0.1.2", 1234, 2345)));
        assertThat(ports, is(expected));
    }

    @Test
    public void two_tcp_port_mappings_results_in_two_ports() throws IOException, InterruptedException {
        String psOutput = "0.0.0.0:5432->5432/tcp, 0.0.0.0:5433->5432/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 5432, 5432),
                                                new DockerPort(LOCALHOST_IP, 5433, 5432)));
        assertThat(ports, is(expected));
    }

    @Test
    public void non_mapped_exposed_port_results_in_no_ports() throws IOException, InterruptedException {
        String psOutput = "5432/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(emptyList());
        assertThat(ports, is(expected));
    }

    @Test
    public void actual_docker_compose_output_can_be_parsed() throws IOException, InterruptedException {
        String psOutput =
                "       Name                      Command               State                                         Ports                                        \n" +
                        "-------------------------------------------------------------------------------------------------------------------------------------------------\n" +
                        "magritte_magritte_1   /bin/sh -c /usr/local/bin/ ...   Up      0.0.0.0:7000->7000/tcp, 7001/tcp, 7002/tcp, 7003/tcp, 7004/tcp, 7005/tcp, 7006/tcp \n" +
                        "";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 7000, 7000)));
        assertThat(ports, is(expected));
    }

    @Test
    public void no_running_container_found_for_service_results_in_an_illegal_state_exception() throws IOException, InterruptedException {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No container found");
        Ports.parseFromDockerComposePs("", "");
    }


    @Test
    public void when_all_ports_are_listening_wait_to_be_listening_returns_without_exception() throws InterruptedException {
        when(port.isListeningNow()).thenReturn(true);
        new Ports(port).waitToBeListeningWithin(Duration.millis(200));
    }

    @Test
    public void when_port_is_unavailable_wait_to_be_listening_throws_an_illegal_state_exception() throws InterruptedException {
        when(port.isListeningNow()).thenReturn(false);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("ConditionTimeoutException"); // Bug in awaitility means it doesn't call hamcrest describeMismatch, this will be "Internal port '7001' mapped to '7000'" was unavailable in practice
        new Ports(port).waitToBeListeningWithin(Duration.millis(200));
    }

    @Test
    public void when_port_becomes_available_after_a_wait_wait_to_be_listening_returns_without_exception() throws InterruptedException {
        when(port.isListeningNow()).thenReturn(false, true);
        new Ports(port).waitToBeListeningWithin(Duration.millis(200));
    }

}
