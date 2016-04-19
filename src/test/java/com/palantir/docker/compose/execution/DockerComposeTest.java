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
package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.Ports;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DockerComposeTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerComposeExecutable executor = mock(DockerComposeExecutable.class);
    private final DockerMachine dockerMachine = mock(DockerMachine.class);
    private final DockerCompose compose = new DefaultDockerCompose(executor, dockerMachine);

    private final Process executedProcess = mock(Process.class);

    @Before
    public void setup() throws IOException, InterruptedException {
        when(dockerMachine.getIp()).thenReturn("0.0.0.0");
        when(executor.execute(anyVararg())).thenReturn(executedProcess);
        when(executedProcess.getInputStream()).thenReturn(toInputStream("0.0.0.0:7000->7000/tcp"));
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
        when(executedProcess.getInputStream()).thenReturn(toInputStream("ps\n----\ndir_db_1"));
        ContainerNames containerNames = compose.ps();
        verify(executor).execute("ps");
        assertThat(containerNames, is(new ContainerNames("db")));
    }

    @Test
    public void logs_calls_docker_compose_with_no_colour_flag() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(toInputStream("logs"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        compose.writeLogs("db", output);
        verify(executor).execute("logs", "--no-color", "db");
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8), is("logs"));
    }

    @Test
    public void when_kill_exits_with_a_non_zero_exit_code_an_exception_is_thrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        exception.expect(DockerComposeExecutionException.class);
        exception.expectMessage("'docker-compose kill' returned exit code 1");
        compose.kill();
    }

    @Test
    public void when_down_fails_because_the_command_does_not_exist_then_an_exception_is_not_thrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        when(executedProcess.getInputStream()).thenReturn(toInputStream("No such command: down"));
        compose.down();
    }

    @Test
    public void when_down_fails_for_a_reason_other_than_the_command_not_being_present_then_an_exception_is_thrown() throws IOException, InterruptedException {
        when(executedProcess.exitValue()).thenReturn(1);
        when(executedProcess.getInputStream()).thenReturn(toInputStream(""));

        exception.expect(DockerComposeExecutionException.class);

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
        when(executedProcess.getInputStream()).thenReturn(toInputStream(""));
        exception.expect(IllegalStateException.class);
        exception.expectMessage("No container with name 'db' found");
        compose.ports("db");
    }

    @Test
    public void docker_compose_exec_pass_concatenated_arguments_to_executor() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(toInputStream("docker-compose version 1.7.0rc1, build 1ad8866"));
        compose.exec(new DockerComposeExecOption(new String[] {"-d"}), "container_1",
                     new DockerComposeExecArgument(new String[] {"ls"}));
        verify(executor).execute("-v");
        verify(executor).execute("exec", "-d", "container_1", "ls");
    }

    @Test
    public void docker_compose_exec_fail_if_docker_compose_version_is_prior_1_7() throws IOException, InterruptedException {

        try {
            when(executedProcess.getInputStream()).thenReturn(toInputStream("docker-compose version 1.5.6, build 1ad8866"));
            compose.exec(new DockerComposeExecOption(new String[] {"-d"}), "container_1",
                         new DockerComposeExecArgument(new String[] {"ls"}));
            fail();
        } catch (IllegalStateException e) {
            //expected
        }
    }

}
