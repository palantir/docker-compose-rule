package com.palantir.docker.compose.connection.waiting;

@FunctionalInterface
public interface Attempt {
    boolean attempt() throws Exception;
}
