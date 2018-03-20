/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.configuration;

import com.palantir.docker.compose.execution.AggressiveShutdownFunction;
import com.palantir.docker.compose.execution.AggressiveShutdownWithNetworkCleanupFunction;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.GracefulShutdownFunction;
import com.palantir.docker.compose.execution.KillDownShutdownFunction;
import com.palantir.docker.compose.execution.SkipShutdownFunction;
import java.io.IOException;

/**
 * How should a cluster of containers be shut down by the `after` method of
 * DockerComposeRule.
 */
public enum ShutdownStrategy implements ShutdownFunction {
    /**
     * Call rm on all containers, working around btrfs bug on CircleCI.
     *
     * @deprecated Use {@link #KILL_DOWN} (the default strategy)
     */
    @Deprecated
    AGGRESSIVE(new AggressiveShutdownFunction()),
    /**
     * Call rm on all containers, then call docker-compose down.
     *
     * @deprecated Use {@link #KILL_DOWN} (the default strategy)
     */
    @Deprecated
    AGGRESSIVE_WITH_NETWORK_CLEANUP(new AggressiveShutdownWithNetworkCleanupFunction()),
    /**
     * Call docker-compose down, kill, then rm. Allows containers up to 10 seconds to shut down
     * gracefully.
     * <p>
     * <p>With this strategy, you will need to take care not to accidentally write images
     * that ignore their down signal, for instance by putting their run command in as a
     * string (which is interpreted by a SIGTERM-ignoring bash) rather than an array of strings.
     */
    GRACEFUL(new GracefulShutdownFunction()),
    /**
     * Call docker-compose kill then down.
     */
    KILL_DOWN(new KillDownShutdownFunction()),
    /**
     * Skip shutdown, leaving containers running after tests finish executing.
     * <p>
     * <p>You can use this option to speed up repeated test execution locally by leaving
     * images up between runs. Do <b>not</b> commit it! You will be potentially abandoning
     * long-running processes and leaking resources on your CI platform!
     */
    SKIP(new SkipShutdownFunction());

    private ShutdownFunction delegate;

    ShutdownStrategy(ShutdownFunction delegate) {
        this.delegate = delegate;
    }

    @Override
    public void shutdown(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException {
        delegate.shutdown(dockerCompose, docker);
    }

}
