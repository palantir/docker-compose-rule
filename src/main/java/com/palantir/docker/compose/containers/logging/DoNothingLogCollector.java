package com.palantir.docker.compose.containers.logging;

import com.palantir.docker.compose.containers.LogCollector;
import com.palantir.docker.compose.execution.DockerComposeExecutable;

public class DoNothingLogCollector implements LogCollector {

    @Override
    public void startCollecting(DockerComposeExecutable dockerCompose) {

    }

    @Override
    public void stopCollecting() {

    }

}
