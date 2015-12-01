package com.palantir.docker.compose;

import java.io.IOException;

public class DoNothingLogCollector implements LogCollector {

    @Override
    public void startCollecting(DockerComposeExecutable dockerCompose) throws IOException, InterruptedException {

    }

    @Override
    public void stopCollecting() throws InterruptedException {

    }

}
