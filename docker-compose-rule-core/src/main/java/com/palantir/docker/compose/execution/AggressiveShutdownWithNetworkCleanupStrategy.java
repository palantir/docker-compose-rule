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
    public void shutdown(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException {
        List<ContainerName> runningContainers = dockerCompose.ps();

        log.info("Shutting down {}", runningContainers.stream().map(ContainerName::semanticName).collect(toList()));
        removeContainersCatchingErrors(docker, runningContainers);
        removeNetworks(dockerCompose, docker);

    }

    private static void removeContainersCatchingErrors(Docker docker, List<ContainerName> runningContainers) throws IOException, InterruptedException {
        try {
            removeContainers(docker, runningContainers);
        } catch (DockerExecutionException exception) {
            log.error("Error while trying to remove containers: {}", exception.getMessage());
        }
    }

    private static void removeContainers(Docker docker, List<ContainerName> running) throws IOException, InterruptedException {
        List<String> rawContainerNames = running.stream()
                .map(ContainerName::rawName)
                .collect(toList());

        docker.rm(rawContainerNames);
        log.debug("Finished shutdown");
    }

    private static void removeNetworks(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException {
        dockerCompose.down();
        docker.pruneNetworks();
    }
}
