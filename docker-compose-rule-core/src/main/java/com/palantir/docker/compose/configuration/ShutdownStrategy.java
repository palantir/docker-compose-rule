/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.configuration;

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

    /**
     * Call docker-compose stop, kill, then down. Allows containers up to 10 seconds to shut down gracefully.
     *
     * <p>With this strategy, you will need to take care not to accidentally write images
     * that ignore their down signal, for instance by putting their run command in as a
     * string (which is interpreted by a SIGTERM-ignoring bash) rather than an array of strings.
     */
    ShutdownStrategy GRACEFUL = new GracefulShutdownStrategy();
    /**
     * Call docker-compose kill then down.
     */
    ShutdownStrategy KILL_DOWN = new KillDownShutdownStrategy();
    /**
     * Skip down, leaving containers running after tests finish executing.
     *
     * <p>You can use this option to speed up repeated test execution locally by leaving
     * images up between runs. Do <b>not</b> commit it! You will be potentially abandoning
     * long-running processes and leaking resources on your CI platform!
     */
    ShutdownStrategy SKIP = new SkipShutdownStrategy();

    void stop(DockerCompose dockerCompose) throws IOException, InterruptedException;

    void down(DockerCompose dockerCompose) throws IOException, InterruptedException;

}
