package com.palantir.docker.compose.execution;

public class DockerComposeExecutionException extends RuntimeException {
    public DockerComposeExecutionException() {
    }

    public DockerComposeExecutionException(String message) {
        super(message);
    }
}
