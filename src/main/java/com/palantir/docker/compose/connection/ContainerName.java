/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

import org.immutables.value.Value;

@Value.Immutable
public abstract class ContainerName {

    public abstract String semanticName();

    public abstract String rawName();

    @Override
    public String toString() {
        return semanticName();
    }
}
