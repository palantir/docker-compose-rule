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

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SynchronousDockerComposeExecutableShould {
    @Mock private Process executedProcess;
    @Mock private DockerComposeExecutable dockerComposeExecutable;
    private SynchronousDockerComposeExecutable dockerCompose;
    private final List<String> consumedLogLines = new ArrayList<>();
    private final Consumer<String> logConsumer = s -> consumedLogLines.add(s);

    @Before
    public void setup() throws IOException {
        when(dockerComposeExecutable.execute(anyVararg())).thenReturn(executedProcess);
        dockerCompose = new SynchronousDockerComposeExecutable(dockerComposeExecutable, logConsumer);

        givenTheUnderlyingProcessHasOutput("");
        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(0);
    }

    @Test public void
    respond_with_the_exit_code_of_the_executed_process() throws IOException, InterruptedException {
        int expectedExitCode = 1;

        givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(expectedExitCode);

        assertThat(dockerCompose.run("rm", "-f").exitCode(), is(expectedExitCode));
    }

    @Test public void
    respond_with_the_output_of_the_executed_process() throws IOException, InterruptedException {
        String expectedOutput = "some output";

        givenTheUnderlyingProcessHasOutput(expectedOutput);

        assertThat(dockerCompose.run("rm", "-f").output(), is(expectedOutput));
    }

    @Test public void
    give_the_output_to_the_specified_consumer_as_it_is_available() throws IOException, InterruptedException {
        givenTheUnderlyingProcessHasOutput("line 1\nline 2");

        dockerCompose.run("rm", "-f");

        assertThat(consumedLogLines, contains("line 1", "line 2"));
    }

    private void givenTheUnderlyingProcessHasOutput(String output) {
        when(executedProcess.getInputStream()).thenReturn(toInputStream(output));
    }

    private void givenTheUnderlyingProcessTerminatesWithAnExitCodeOf(int exitCode) {
        when(executedProcess.exitValue()).thenReturn(exitCode);
    }

}
