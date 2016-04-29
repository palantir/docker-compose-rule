/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection.waiting;

import static java.util.stream.Collectors.toList;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerAccessor;
import java.util.List;
import org.immutables.value.Value;
import org.joda.time.Duration;

@Value.Immutable
public abstract class MultiServiceWait implements ClusterWait {

    @Value.Parameter
    protected abstract List<String> containerNames();

    @Value.Parameter
    protected abstract MultiServiceHealthCheck healthcheck();

    @Value.Parameter
    protected abstract Duration timeout();

    public static MultiServiceWait of(List<String> serviceNames, MultiServiceHealthCheck healthCheck, Duration timeout) {
        return ImmutableMultiServiceWait.of(serviceNames, healthCheck, timeout);
    }

    @Override
    public void waitUntilReady(ContainerAccessor containers) {
        List<Container> containersToWaitFor = containerNames().stream()
                        .map(containers::container)
                        .collect(toList());
        ServiceWait serviceWait = new ServiceWait(containersToWaitFor, healthcheck(), timeout());
        serviceWait.waitTillServiceIsUp();
    }

}
