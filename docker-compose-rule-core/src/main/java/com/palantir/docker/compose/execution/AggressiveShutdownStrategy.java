/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;


import com.palantir.docker.compose.configuration.ShutdownStrategy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shuts down containers as fast as possible, without giving them time to finish
 * IO or clean up any resources.
 *
 * @deprecated Use {@link ShutdownStrategy#KILL_DOWN}
 */
@Deprecated
public class AggressiveShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(AggressiveShutdownStrategy.class);

    @Override
    public void stop(DockerCompose dockerCompose) throws IOException, InterruptedException {
        log.debug("Killing docker-compose cluster");
        dockerCompose.kill();
    }

    @Override
    public void down(DockerCompose dockerCompose) throws IOException, InterruptedException {
        log.debug("Downing docker-compose cluster");
        dockerCompose.down();
    }
}
