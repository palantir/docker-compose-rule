package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.Ports;

import java.io.IOException;
import java.io.OutputStream;

public interface DockerCompose {
    void build() throws IOException, InterruptedException;
    void up() throws IOException, InterruptedException;
    void down() throws IOException, InterruptedException;
    void kill() throws IOException, InterruptedException;
    void rm() throws IOException, InterruptedException;
    ContainerNames ps() throws IOException, InterruptedException;
    Container container(String containerName);
    boolean writeLogs(String container, OutputStream output) throws IOException;
    Ports ports(String service) throws IOException, InterruptedException;
}
