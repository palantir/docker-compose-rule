/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

import static java.util.stream.Collectors.toList;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
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

}
