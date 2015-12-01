package com.palantir.docker.compose;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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

public class DockerComposeExecutableTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerComposeExecutor executor = mock(DockerComposeExecutor.class);
    private final DockerComposeExecutable compose = new DockerComposeExecutable(executor);

    private final Process executedProcess = mock(Process.class);

    @Before
    public void setup() throws IOException, InterruptedException {
        when(executor.executeAndWait(anyVararg())).thenReturn(executedProcess);
        when(executor.execute(anyVararg())).thenReturn(executedProcess);
        when(executedProcess.getInputStream()).thenReturn(new ByteArrayInputStream("0.0.0.0:7000->7000/tcp".getBytes(StandardCharsets.UTF_8)));
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
        when(executedProcess.getInputStream()).thenReturn(new ByteArrayInputStream("ps\n----\ndir_db_1".getBytes(StandardCharsets.UTF_8)));
        ContainerNames containerNames = compose.ps();
        verify(executor).executeAndWait("ps");
        assertThat(containerNames, is(new ContainerNames("db")));
    }

    @Test
    public void logsCallsDockerComposeWithNoColourFlag() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(new ByteArrayInputStream("logs".getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        compose.writeLogs("db", output);
        verify(executor).execute("logs", "--no-color", "db");
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8), is("logs"));
    }

    @Test
    public void whenTheExitValueIsOneAnISEIsThrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        exception.expect(IllegalStateException.class);
        exception.expectMessage("'docker-compose kill' returned exit code 1");
        compose.kill();
    }

    @Test
    public void callingPortsParsesThePsOutput() throws IOException, InterruptedException {
        PortMappings ports = compose.ports("db");
        verify(executor).executeAndWait("ps", "db");
        assertThat(ports, is(new PortMappings(new PortMapping(7000, 7000))));
    }

    @Test
    public void whenThereIsNoContainerFoundForPortsAnISEIsThrown() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        exception.expect(IllegalStateException.class);
        exception.expectMessage("No container with name 'db' found");
        compose.ports("db");
    }

}
