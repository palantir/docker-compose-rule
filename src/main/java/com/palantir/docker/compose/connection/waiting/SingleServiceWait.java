/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection.waiting;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerAccessor;
import org.immutables.value.Value;
import org.joda.time.Duration;

@Value.Immutable
public abstract class SingleServiceWait {

    @Value.Parameter
    public abstract String serviceName();

    @Value.Parameter
    public abstract SingleServiceHealthCheck healthCheck();

    public static SingleServiceWait of(String serviceName, SingleServiceHealthCheck healthCheck) {
        return ImmutableSingleServiceWait.of(serviceName, healthCheck);
    }

    public void waitUntilReady(ContainerAccessor containers, Duration timeout) {
        Container container = containers.container(serviceName());
        ServiceWait serviceWait = new ServiceWait(container, healthCheck(), timeout);
        serviceWait.waitTillServiceIsUp();
    }

}
