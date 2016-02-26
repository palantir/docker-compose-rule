package com.palantir.docker.compose.execution;

@FunctionalInterface
public interface ErrorHandler {
    void handle(int exitCode, String output, String... commands);
}
