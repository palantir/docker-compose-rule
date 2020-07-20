/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shuts down fast but cleanly by issuing a kill (fast shutdown) followed by a down (thorough cleanup)
 *
 * <p>"down" would be ideal as a single command if it didn't first execute an impotent SIGTERM, which
 * many Docker images simply ignore due to being run by bash as process 1. We don't need a graceful
 * shutdown period anyway since the tests are done and we're destroying the docker image.
 */
public class KillDownShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(KillDownShutdownStrategy.class);

    @Override
    public void stop(DockerCompose dockerCompose) throws IOException, InterruptedException {
        log.debug("Killing docker-compose cluster");
        dockerCompose.kill();
    }

    @Override
    public void shutdown(DockerCompose dockerCompose, Docker _docker) throws IOException, InterruptedException {
        log.debug("Downing docker-compose cluster");
        dockerCompose.down();
    }
}
