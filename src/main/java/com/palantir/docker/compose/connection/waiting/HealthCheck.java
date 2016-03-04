package com.palantir.docker.compose.connection.waiting;

import com.palantir.docker.compose.connection.Container;

@FunctionalInterface
public interface HealthCheck {
    boolean isServiceUp(Container container);
}
