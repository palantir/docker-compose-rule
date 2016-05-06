/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Cluster {

    public abstract String ip();
    public abstract ContainerCache containers();

    public Container container(String name) {
        return containers().container(name);
    }

}
