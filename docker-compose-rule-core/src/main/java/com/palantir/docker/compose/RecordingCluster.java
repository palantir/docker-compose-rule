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

package com.palantir.docker.compose;

import com.google.common.collect.Sets;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class RecordingCluster extends Cluster {
    private final Cluster delegate;
    private final Set<String> recordedContainerNames = ConcurrentHashMap.newKeySet();

    RecordingCluster(Cluster delegate) {
        this.delegate = delegate;
    }

    @Override
    public String ip() {
        return delegate.ip();
    }

    @Override
    public ContainerCache containerCache() {
        return delegate.containerCache();
    }

    @Override
    public Container container(String name) {
        recordedContainerNames.add(name);
        return delegate.container(name);
    }

    @Override
    public List<Container> containers(List<String> containerNames) {
        recordedContainerNames.addAll(containerNames);
        return delegate.containers(containerNames);
    }

    @Override
    public Set<Container> allContainers() throws IOException, InterruptedException {
        Set<Container> containers = delegate.allContainers();
        containers.forEach(container -> recordedContainerNames.add(container.getContainerName()));
        return containers;
    }

    public Set<String> recordedContainerNames() {
        return recordedContainerNames;
    }
}
