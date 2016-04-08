package com.palantir.docker.compose.execution;

public class DockerComposeExecutionException extends RuntimeException {
    public DockerComposeExecutionException() {
    }

    public DockerComposeExecutionException(String message) {
        super(message);
    }

    public DockerComposeExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerComposeExecutionException(Throwable cause) {
        super(cause);
    }
}
