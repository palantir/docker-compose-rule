package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.Ports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class RetryingDockerCompose implements DockerCompose {
    private static final Logger log = LoggerFactory.getLogger(RetryingDockerCompose.class);

    private final int attempts;
    private final DockerCompose dockerCompose;

    public RetryingDockerCompose(int attempts, DockerCompose dockerCompose) {
        this.attempts = attempts;
        this.dockerCompose = dockerCompose;
    }

    @Override
    public void build() throws IOException, InterruptedException {
        dockerCompose.build();
    }

    @Override
    public void up() throws IOException, InterruptedException {
        DockerComposeExecutionException lastExecutionException = null;
        for (int i = 0; i < attempts; i++) {
            try {
                dockerCompose.up();
                return;
            } catch (DockerComposeExecutionException e) {
                lastExecutionException = e;
                log.warn("Caught exception: " + e.getMessage() + ". Retrying operation.");
            }
        }

        throw lastExecutionException;
    }

    @Override
    public void down() throws IOException, InterruptedException {
        dockerCompose.down();
    }

    @Override
    public void kill() throws IOException, InterruptedException {
        dockerCompose.kill();
    }

    @Override
    public void rm() throws IOException, InterruptedException {
        dockerCompose.rm();
    }

    @Override
    public ContainerNames ps() throws IOException, InterruptedException {
        return dockerCompose.ps();
    }

    @Override
    public Container container(String containerName) {
        return dockerCompose.container(containerName);
    }

    @Override
    public boolean writeLogs(String container, OutputStream output) throws IOException {
        return dockerCompose.writeLogs(container, output);
    }

    @Override
    public Ports ports(String service) throws IOException, InterruptedException {
        return dockerCompose.ports(service);
    }
}
