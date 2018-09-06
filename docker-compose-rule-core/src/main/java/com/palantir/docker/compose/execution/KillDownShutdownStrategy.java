/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send SIGKILL to containers to kill them quickly.
 */
public class KillDownShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(KillDownShutdownStrategy.class);

    @Override
    public void stop(DockerCompose dockerCompose) throws IOException, InterruptedException {
        log.debug("Killing docker-compose cluster");
        dockerCompose.kill();
    }

    @Override
    public void down(DockerCompose dockerCompose)
            throws IOException, InterruptedException {
        log.debug("Downing docker-compose cluster");
        dockerCompose.down();
    }
}
