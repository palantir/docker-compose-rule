/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose;

import com.palantir.docker.compose.connection.waiting.SingleServiceHealthCheck;
import org.immutables.value.Value;

@Value.Immutable
public interface DockerService {

    @Value.Parameter
    String serviceName();

    @Value.Parameter
    SingleServiceHealthCheck healthCheck();

    static DockerService of(String serviceName, SingleServiceHealthCheck healthCheck) {
        return ImmutableDockerService.of(serviceName, healthCheck);
    }
}
