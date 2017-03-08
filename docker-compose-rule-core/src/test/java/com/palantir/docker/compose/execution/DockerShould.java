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
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.zafarkhaja.semver.Version;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class DockerShould {

    private final DockerExecutable executor = mock(DockerExecutable.class);
    private final Docker docker = new Docker(executor);

    private final Process executedProcess = mock(Process.class);

    @Before
    public void setup() throws IOException {
        when(executor.execute(anyVararg())).thenReturn(executedProcess);
        when(executedProcess.exitValue()).thenReturn(0);
    }

    @Test
    public void call_docker_rm_with_force_flag_on_rm() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(toInputStream(""));

        docker.rm("testContainer");

        verify(executor).execute("rm", "-f", "testContainer");
    }

    @Test
    public void call_docker_network_ls() throws IOException, InterruptedException {
        String lsOutput = "0.0.0.0:7000->7000/tcp";
        when(executedProcess.getInputStream()).thenReturn(toInputStream(lsOutput));

        assertThat(docker.listNetworks(), is(lsOutput));

        verify(executor).execute("network", "ls");
    }

    @Test
    public void understand_old_version_format() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(toInputStream("Docker version 1.7.2"));

        Version version = docker.configuredVersion();
        assertThat(version, is(Version.valueOf("1.7.2")));
    }

    @Test
    public void understand_new_version_format() throws IOException, InterruptedException {
        when(executedProcess.getInputStream()).thenReturn(toInputStream("Docker version 17.03.1-ce"));

        Version version = docker.configuredVersion();
        assertThat(version, is(Version.valueOf("17.3.1")));
    }
}
