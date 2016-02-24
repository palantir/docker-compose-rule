package com.palantir.docker.compose.containers;

import com.palantir.docker.compose.execution.DockerComposeExecutable;
import java.io.IOException;

public interface LogCollector {

    void startCollecting(DockerComposeExecutable dockerCompose) throws IOException, InterruptedException;

    void stopCollecting() throws InterruptedException;

}