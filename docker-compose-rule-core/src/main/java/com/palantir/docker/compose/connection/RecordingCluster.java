/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class RecordingCluster extends Cluster {
    private final Cluster realCluster;
    private final Set<String> recordedContainerNames = Sets.newConcurrentHashSet();

    public RecordingCluster(Cluster realCluster) {
        this.realCluster = realCluster;
    }

    @Override
    public String ip() {
        return realCluster.ip();
    }

    @Override
    public ContainerCache containerCache() {
        return realCluster.containerCache();
    }

    @Override
    public Container container(String name) {
        recordedContainerNames.add(name);
        return realCluster.container(name);
    }

    @Override
    public List<Container> containers(List<String> containerNames) {
        recordedContainerNames.addAll(containerNames);
        return realCluster.containers(containerNames);
    }

    @Override
    public Set<Container> allContainers() throws IOException, InterruptedException {
        Set<Container> containers = realCluster.allContainers();
        containers.forEach(container -> recordedContainerNames.add(container.getContainerName()));
        return containers;
    }

    public Set<String> recordedContainerNames() {
        return recordedContainerNames;
    }
}
