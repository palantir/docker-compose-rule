/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.docker.compose.connection;

import static java.util.stream.Collectors.toSet;

import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ContainerCache {

    private final ConcurrentMap<String, Container> containers = new ConcurrentHashMap<>();
    private final Docker docker;
    private final DockerCompose dockerCompose;

    public ContainerCache(Docker docker, DockerCompose dockerCompose) {
        this.docker = docker;
        this.dockerCompose = dockerCompose;
    }

    public Container container(String containerName) {
        return containers.computeIfAbsent(
                containerName, _ignored -> new Container(containerName, docker, dockerCompose));
    }

    public Set<Container> containers() throws IOException, InterruptedException {
        return dockerCompose.services().stream().map(this::container).collect(toSet());
    }
}
