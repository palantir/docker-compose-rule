/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shuts down containers as fast as possible, without giving them time to finish
 * IO or clean up any resources.
 */
public class AggressiveShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(AggressiveShutdownStrategy.class);

    @Override
    public void shutdown(DockerCompose dockerCompose) throws IOException, InterruptedException {
        log.info("Shutting down");
        dockerCompose.rm();
        log.debug("Finished shutdown");
    }

}
