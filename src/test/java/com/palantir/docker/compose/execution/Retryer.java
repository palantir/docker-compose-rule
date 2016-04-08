package com.palantir.docker.compose.execution;

public class Retryer {
    private final int attempts;

    public Retryer(int attempts) {
        this.attempts = attempts;
    }


}
