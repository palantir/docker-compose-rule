package com.palantir.docker.compose.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Retryer {
    private static final Logger log = LoggerFactory.getLogger(Retryer.class);

    public interface RetryableDockerComposeOperation<T> {
        T call() throws IOException, InterruptedException;
    }

    private final int retryAttempts;

    public Retryer(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public <T> T runWithRetries(RetryableDockerComposeOperation<T> operation) throws IOException, InterruptedException {
        DockerComposeExecutionException lastExecutionException = null;
        for (int i = 0; i <= retryAttempts; i++) {
            try {
                return operation.call();
            } catch (DockerComposeExecutionException e) {
                lastExecutionException = e;
                log.warn("Caught exception: " + e.getMessage() + ". Retrying.");
            }
        }

        throw lastExecutionException;
    }
}
