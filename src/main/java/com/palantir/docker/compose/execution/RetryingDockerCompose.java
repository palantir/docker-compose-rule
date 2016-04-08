package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.Ports;

import java.io.IOException;
import java.io.OutputStream;

public class RetryingDockerCompose implements DockerCompose {
    private final Retryer retryer;
    private final DockerCompose dockerCompose;

    public RetryingDockerCompose(int retryAttempts, DockerCompose dockerCompose) {
        this(new Retryer(retryAttempts), dockerCompose);
    }

    public RetryingDockerCompose(Retryer retryer, DockerCompose dockerCompose) {
        this.retryer = retryer;
        this.dockerCompose = dockerCompose;
    }

    @Override
    public void build() throws IOException, InterruptedException {
        dockerCompose.build();
    }

    @Override
    public void up() throws IOException, InterruptedException {
        retryer.<Void>runWithRetries(() -> {
            dockerCompose.up();
            return null;
        });
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
        return retryer.runWithRetries(dockerCompose::ps);
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
