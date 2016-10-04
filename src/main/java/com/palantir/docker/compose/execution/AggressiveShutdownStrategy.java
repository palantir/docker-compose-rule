/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static java.util.stream.Collectors.toList;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.ContainerName;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shuts down containers as fast as possible, without giving them time to finish
 * IO or clean up any resources.
 */
public class AggressiveShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(AggressiveShutdownStrategy.class);

    @Override
    public void shutdown(DockerComposeRule rule) throws IOException, InterruptedException {
        List<ContainerName> runningContainers = rule.dockerCompose().ps();

        log.info("Shutting down {}", runningContainers.stream().map(ContainerName::semanticName).collect(toList()));
        if (removeContainersCatchingErrors(rule, runningContainers)) {
            return;
        }

        log.debug("First shutdown attempted failed due to btrfs volume error... retrying");
        if (removeContainersCatchingErrors(rule, runningContainers)) {
            return;
        }

        log.warn("Couldn't shut down containers due to btrfs volume error, "
                + "see https://circleci.com/docs/docker-btrfs-error/ for more info.");
    }

    private boolean removeContainersCatchingErrors(DockerComposeRule rule, List<ContainerName> runningContainers) throws IOException, InterruptedException {
        try {
            removeContainers(rule, runningContainers);
            return true;
        } catch (DockerExecutionException exception) {
            return false;
        }
    }

    private void removeContainers(DockerComposeRule rule, List<ContainerName> running) throws IOException, InterruptedException {
        List<String> rawContainerNames = running.stream()
                .map(ContainerName::rawName)
                .collect(toList());

        rule.docker().rm(rawContainerNames);
        log.debug("Finished shutdown");
    }

}
