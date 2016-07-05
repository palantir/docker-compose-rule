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

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.Ports;
import com.palantir.docker.compose.connection.State;
import java.io.IOException;
import java.io.OutputStream;

abstract class DelegatingDockerCompose implements DockerCompose {
    private final DockerCompose dockerCompose;

    protected DelegatingDockerCompose(DockerCompose dockerCompose) {
        this.dockerCompose = dockerCompose;
    }

    @Override
    public void build() throws IOException, InterruptedException {
        dockerCompose.build();
    }

    @Override
    public void up() throws IOException, InterruptedException {
        dockerCompose.up();
    }

    @Override
    public void down() throws IOException, InterruptedException {
        dockerCompose.down();
    }

    @Override
    public void kill() throws IOException, InterruptedException {
        dockerCompose.kill();
    }

    @Override
    public void rm() throws IOException, InterruptedException {
        dockerCompose.rm();
    }

    @Override
    public void up(Container container) throws IOException, InterruptedException {
        dockerCompose.up(container);
    }

    @Override
    public void start(Container container) throws IOException, InterruptedException {
        dockerCompose.start(container);
    }

    @Override
    public void stop(Container container) throws IOException, InterruptedException {
        dockerCompose.stop(container);
    }

    @Override
    public String exec(DockerComposeExecOption dockerComposeExecOption, String containerName,
            DockerComposeExecArgument dockerComposeExecArgument) throws IOException, InterruptedException {
        return dockerCompose.exec(dockerComposeExecOption, containerName, dockerComposeExecArgument);
    }

    @Override
    public ContainerNames ps() throws IOException, InterruptedException {
        return dockerCompose.ps();
    }

    @Override
    public Container container(String containerName) {
        return dockerCompose.container(containerName);
    }

    @Override
    public boolean writeLogs(String container, OutputStream output) throws IOException {
        return dockerCompose.writeLogs(container, output);
    }

    @Override
    public Ports ports(String service) throws IOException, InterruptedException {
        return dockerCompose.ports(service);
    }

    @Override
    public State state(String service) throws IOException, InterruptedException {
        return dockerCompose.state(service);
    }

    protected final DockerCompose getDockerCompose() {
        return dockerCompose;
    }

}
