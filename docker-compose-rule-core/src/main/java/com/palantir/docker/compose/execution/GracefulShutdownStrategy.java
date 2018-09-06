/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send SIGTERM to containers allowing them up to 10 seconds to terminate. Then send SIGILL to containers to kill them.
 */
public class GracefulShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownStrategy.class);

    @Override
    public void stop(DockerCompose dockerCompose) throws IOException, InterruptedException {
        log.debug("Stopping docker-compose cluster");
        dockerCompose.stop();
        dockerCompose.kill();
    }

    @Override
    public void down(DockerCompose dockerCompose) throws IOException, InterruptedException {
        log.debug("Downing docker-compose cluster");
        dockerCompose.down();
    }

}
