package com.palantir.docker.compose;

public class DoNothingLogCollector implements LogCollector {

    @Override
    public void startCollecting(DockerComposeExecutable dockerCompose) {

    }

    @Override
    public void stopCollecting() {

    }

}
