/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.configuration;

import com.palantir.docker.compose.execution.AggressiveShutdownStrategy;
import com.palantir.docker.compose.execution.AggressiveShutdownWithNetworkCleanupStrategy;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.GracefulShutdownStrategy;
import com.palantir.docker.compose.execution.KillDownShutdownStrategy;
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
    ShutdownStrategy AGGRESSIVE_WITH_NETWORK_CLEANUP = new AggressiveShutdownWithNetworkCleanupStrategy();
    ShutdownStrategy KILL_DOWN = new KillDownShutdownStrategy();

    void shutdown(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException;

}
