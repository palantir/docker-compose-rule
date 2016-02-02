package com.palantir.docker.compose.logging;

import com.palantir.docker.compose.execution.DockerComposeExecutable;
import java.io.IOException;

public class DoNothingLogCollector implements LogCollector {

    @Override
    public void startCollecting(DockerComposeExecutable dockerCompose) throws IOException, InterruptedException {

    }

    @Override
    public void stopCollecting() throws InterruptedException {

    }

}
