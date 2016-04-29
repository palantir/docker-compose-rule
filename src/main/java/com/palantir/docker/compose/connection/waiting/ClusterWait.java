/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection.waiting;

import com.palantir.docker.compose.connection.ContainerAccessor;
import org.joda.time.Duration;

@FunctionalInterface
public interface ClusterWait {

    void waitUntilReady(ContainerAccessor containers, Duration timeout);

}
