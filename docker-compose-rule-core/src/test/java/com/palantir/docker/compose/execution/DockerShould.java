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
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class DockerShould {

    private final DockerExecutable executor = mock(DockerExecutable.class);
    private final Docker docker = new Docker(executor);

    private final Process executedProcess = mock(Process.class);

    @Before
    public void setup() throws IOException, InterruptedException {
        when(executor.execute(anyVararg())).thenReturn(executedProcess);
        when(executedProcess.getInputStream()).thenReturn(toInputStream("0.0.0.0:7000->7000/tcp"));
        when(executedProcess.exitValue()).thenReturn(0);
    }

    @Test
    public void call_docker_rm_with_force_flag_on_rm() throws IOException, InterruptedException {
        docker.rm("testContainer");

        verify(executor).execute("rm", "-f", "testContainer");
    }

    @Test
    public void call_docker_network_ls() throws IOException, InterruptedException {
        docker.listNetworks();

        verify(executor).execute("network", "ls");
    }

}
