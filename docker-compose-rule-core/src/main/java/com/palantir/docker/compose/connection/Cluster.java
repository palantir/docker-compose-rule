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

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@SuppressWarnings("DesignForExtension")
public abstract class Cluster {

    public abstract String ip();
    public abstract ContainerCache containerCache();

    public Container container(String name) {
        return containerCache().container(name);
    }

    public List<Container> containers(List<String> containerNames) {
        return containerNames.stream()
                .map(this::container)
                .collect(toList());
    }

    public Set<Container> allContainers() throws IOException, InterruptedException {
        return containerCache().containers();
    }

}
