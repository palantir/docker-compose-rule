package com.palantir.docker.compose;

import java.io.IOException;

public interface LogCollector {

    void startCollecting(DockerComposeExecutable dockerCompose) throws IOException, InterruptedException;

    void stopCollecting() throws InterruptedException;

}