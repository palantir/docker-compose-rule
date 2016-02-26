package com.palantir.docker.compose.logging;

import java.io.IOException;

import com.palantir.docker.compose.execution.DockerComposeExecutable;

public interface LogCollector {

    void startCollecting(DockerComposeExecutable dockerCompose) throws IOException, InterruptedException;

    void stopCollecting() throws InterruptedException;

}