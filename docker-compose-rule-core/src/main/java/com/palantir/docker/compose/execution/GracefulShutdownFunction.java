/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.configuration.ShutdownFunction;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send SIGTERM to containers first, allowing them up to 10 seconds to
 * terminate before killing and rm-ing them.
 */
public class GracefulShutdownFunction implements ShutdownFunction {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownFunction.class);

    @Override
    public void shutdown(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException {
        log.debug("Killing docker-compose cluster");
        dockerCompose.down();
        dockerCompose.kill();
        dockerCompose.rm();
    }

}
