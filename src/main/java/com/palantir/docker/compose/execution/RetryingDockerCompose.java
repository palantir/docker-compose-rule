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
import java.io.IOException;
import java.io.OutputStream;

public class RetryingDockerCompose implements DockerCompose {
    private final Retryer retryer;
    private final DockerCompose dockerCompose;

    public RetryingDockerCompose(int retryAttempts, DockerCompose dockerCompose) {
        this(new Retryer(retryAttempts), dockerCompose);
    }

    public RetryingDockerCompose(Retryer retryer, DockerCompose dockerCompose) {
        this.retryer = retryer;
        this.dockerCompose = dockerCompose;
    }

    @Override
    public void build() throws IOException, InterruptedException {
        dockerCompose.build();
    }

    @Override
    public void up() throws IOException, InterruptedException {
        retryer.<Void>runWithRetries(() -> {
            dockerCompose.up();
            return null;
        });
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
    public ContainerNames ps() throws IOException, InterruptedException {
        return retryer.runWithRetries(dockerCompose::ps);
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
}
