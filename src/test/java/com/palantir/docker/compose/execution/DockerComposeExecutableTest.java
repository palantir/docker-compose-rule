package com.palantir.docker.compose.execution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.Ports;

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
