/*
 * Copyright 2018 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.configuration;

import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import java.io.IOException;

@FunctionalInterface
public interface ShutdownFunction {

    void shutdown(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException;

}
