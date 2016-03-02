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
package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.Ports;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DockerComposeExecutableTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerComposeExecutor executor = mock(DockerComposeExecutor.class);
    private final DockerMachine dockerMachine = mock(DockerMachine.class);
    private final DockerComposeExecutable compose = new DockerComposeExecutable(executor, dockerMachine);

    private final Process executedProcess = mock(Process.class);

    @Before
    public void setup() throws IOException, InterruptedException {
        when(dockerMachine.getIp()).thenReturn("0.0.0.0");
        when(executor.execute(anyVararg())).thenReturn(executedProcess);
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf("0.0.0.0:7000->7000/tcp"));
        when(executedProcess.exitValue()).thenReturn(0);
    }

    @Test
    public void up_calls_docker_compose_up_with_daemon_flag() throws IOException, InterruptedException {
        compose.up();
        verify(executor).execute("up", "-d");
    }

    @Test
    public void rm_calls_docker_compose_rm_with_f_flag() throws IOException, InterruptedException {
        compose.rm();
        verify(executor).execute("rm", "-f");
    }

    @Test
    public void ps_parses_and_returns_container_names() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf("ps\n----\ndir_db_1"));
        ContainerNames containerNames = compose.ps();
        verify(executor).execute("ps");
        assertThat(containerNames, is(new ContainerNames("db")));
    }

    @Test
    public void logs_calls_docker_compose_with_no_colour_flag() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf("logs"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        compose.writeLogs("db", output);
        verify(executor).execute("logs", "--no-color", "db");
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8), is("logs"));
    }

    @Test
    public void when_kill_exits_with_a_non_zero_exit_code_an_exception_is_thrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("'docker-compose kill' returned exit code 1");
        compose.kill();
    }

    @Test
    public void when_down_fails_because_the_command_does_not_exist_then_an_exception_is_not_thrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf("No such command: down"));
        compose.down();
    }

    @Test
    public void when_down_fails_for_a_reason_other_than_the_command_not_being_present_then_an_exception_is_thrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf(""));

        exception.expect(IllegalStateException.class);

        compose.down();
    }

    @Test
    public void calling_ports_parses_the_ps_output() throws IOException, InterruptedException {
        Ports ports = compose.ports("db");
        verify(executor).execute("ps", "db");
        assertThat(ports, is(new Ports(new DockerPort("0.0.0.0", 7000, 7000))));
    }

    @Test
    public void when_there_is_no_container_found_for_ports_an_i_s_e_is_thrown() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf(""));
        exception.expect(IllegalStateException.class);
        exception.expectMessage("No container with name 'db' found");
        compose.ports("db");
    }

    private static ByteArrayInputStream byteArrayInputStreamOf(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

}
