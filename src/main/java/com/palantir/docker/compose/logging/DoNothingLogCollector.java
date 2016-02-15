package com.palantir.docker.compose.logging;

import java.io.IOException;

import com.palantir.docker.compose.execution.DockerComposeExecutable;

public class DoNothingLogCollector implements LogCollector {

    @Override
    public void startCollecting(DockerComposeExecutable dockerCompose) throws IOException, InterruptedException {

    }

    @Override
    public void stopCollecting() throws InterruptedException {

    }

}
