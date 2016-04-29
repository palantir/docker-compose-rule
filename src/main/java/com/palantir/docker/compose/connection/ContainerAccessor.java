/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

public interface ContainerAccessor {

    Container container(String name);

}
