/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection.waiting;

import com.palantir.docker.compose.connection.Cluster;
import org.immutables.value.Value;
import org.joda.time.Duration;

@Value.Immutable
public abstract class SingleServiceWait implements ClusterWait {

    @Value.Parameter
    protected abstract String containerName();

    @Value.Parameter
    protected abstract SingleServiceHealthCheck healthCheck();

    @Value.Parameter
    protected abstract Duration timeout();

    public static SingleServiceWait of(String serviceName, SingleServiceHealthCheck healthCheck, Duration timeout) {
        return ImmutableSingleServiceWait.of(serviceName, healthCheck, timeout);
    }

    @Override
    public void waitUntilReady(Cluster cluster) {
        ServiceWait serviceWait = new ServiceWait(containerName(), healthCheck(), timeout());
        serviceWait.waitUntilReady(cluster);
    }

}
