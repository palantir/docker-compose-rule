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
        when(executor.executeAndWait(anyVararg())).thenReturn(executedProcess);
        when(executor.execute(anyVararg())).thenReturn(executedProcess);
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf("0.0.0.0:7000->7000/tcp"));
        when(executedProcess.exitValue()).thenReturn(0);
    }

    @Test
    public void upCallsDockerComposeUpWithDaemonFlag() throws IOException, InterruptedException {
        compose.up();
        verify(executor).executeAndWait("up", "-d");
    }

    @Test
    public void rmCallsDockerComposeRmWithFFlag() throws IOException, InterruptedException {
        compose.rm();
        verify(executor).executeAndWait("rm", "-f");
    }

    @Test
    public void psParsesAndReturnsContainerNames() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf("ps\n----\ndir_db_1"));
        ContainerNames containerNames = compose.ps();
        verify(executor).executeAndWait("ps");
        assertThat(containerNames, is(new ContainerNames("db")));
    }

    @Test
    public void logsCallsDockerComposeWithNoColourFlag() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf("logs"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        compose.writeLogs("db", output);
        verify(executor).execute("logs", "--no-color", "db");
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8), is("logs"));
    }

    @Test
    public void whenKillExitsWithANonZeroExitCodeAnExceptionIsThrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("'docker-compose kill' returned exit code 1");
        compose.kill();
    }

    @Test
    public void whenDownFailsBecauseTheCommandDoesNotExistAnExceptionIsNotThrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf("No such command: down"));
        compose.down();
    }

    @Test
    public void whenDownFailsBecauseForAReasonOtherThanTheCommandNotBeingPresentThenAnExceptionIsThrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf(""));

        exception.expect(IllegalStateException.class);

        compose.down();
    }

    @Test
    public void callingPortsParsesThePsOutput() throws IOException, InterruptedException {
        Ports ports = compose.ports("db");
        verify(executor).executeAndWait("ps", "db");
        assertThat(ports, is(new Ports(new DockerPort("0.0.0.0", 7000, 7000))));
    }

    @Test
    public void whenThereIsNoContainerFoundForPortsAnISEIsThrown() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(byteArrayInputStreamOf(""));
        exception.expect(IllegalStateException.class);
        exception.expectMessage("No container with name 'db' found");
        compose.ports("db");
    }

    private static ByteArrayInputStream byteArrayInputStreamOf(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

}
