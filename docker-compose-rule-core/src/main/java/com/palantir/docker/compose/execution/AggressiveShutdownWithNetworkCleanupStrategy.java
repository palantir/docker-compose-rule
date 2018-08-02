/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static java.util.stream.Collectors.toList;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.ContainerName;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shuts down containers as fast as possible while cleaning up any networks that were created.
 *
 * @deprecated Use {@link ShutdownStrategy#KILL_DOWN}
 */
@Deprecated
public class AggressiveShutdownWithNetworkCleanupStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(AggressiveShutdownWithNetworkCleanupStrategy.class);

    @Override
    public void stop(DockerCompose dockerCompose) throws IOException, InterruptedException {
        log.debug("Killing docker-compose cluster");
        dockerCompose.kill();
    }

    @Override
    public void shutdown(DockerCompose dockerCompose) throws IOException, InterruptedException {
        List<ContainerName> runningContainers = dockerCompose.ps();

        log.info("Shutting down {}", runningContainers.stream().map(ContainerName::semanticName).collect(toList()));
        removeContainersCatchingErrors(dockerCompose);
        removeNetworks(dockerCompose);
    }

    private static void removeContainersCatchingErrors(DockerCompose dockerCompose) throws IOException, InterruptedException {
        try {
            dockerCompose.rm();
        } catch (DockerExecutionException exception) {
            log.error("Error while trying to remove containers: {}", exception.getMessage());
        }
    }

    private static void removeNetworks(DockerCompose dockerCompose) throws IOException, InterruptedException {
        dockerCompose.down();
        log.debug("Finished shutdown");
    }
}
