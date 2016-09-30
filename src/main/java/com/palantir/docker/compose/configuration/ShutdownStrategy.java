/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.configuration;

import com.palantir.docker.compose.execution.DockerCompose;
import java.io.IOException;

/**
 * How should a cluster of containers be shut down by the `after` method of
 * DockerComposeRule.
 */
public interface ShutdownStrategy {

    void shutdown(DockerCompose dockerCompose) throws IOException, InterruptedException;

}
