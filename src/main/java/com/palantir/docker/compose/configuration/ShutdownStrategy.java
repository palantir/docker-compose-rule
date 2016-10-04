/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.configuration;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.execution.AggressiveShutdownStrategy;
import com.palantir.docker.compose.execution.GracefulShutdownStrategy;
import com.palantir.docker.compose.execution.SkipShutdownStrategy;
import java.io.IOException;

/**
 * How should a cluster of containers be shut down by the `after` method of
 * DockerComposeRule.
 */
public interface ShutdownStrategy {

    ShutdownStrategy AGGRESSIVE = new AggressiveShutdownStrategy();
    ShutdownStrategy GRACEFUL = new GracefulShutdownStrategy();
    ShutdownStrategy SKIP = new SkipShutdownStrategy();

    void shutdown(DockerComposeRule rule) throws IOException, InterruptedException;

}
