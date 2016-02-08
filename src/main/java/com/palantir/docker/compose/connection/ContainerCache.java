package com.palantir.docker.compose.connection;

import java.util.HashMap;
import java.util.Map;

import com.palantir.docker.compose.execution.DockerComposeExecutable;

public class ContainerCache {

    private final Map<String, Container> containers = new HashMap<>();
    private final DockerComposeExecutable dockerComposeExecutable;

    public ContainerCache(DockerComposeExecutable dockerComposeExecutable) {
        this.dockerComposeExecutable = dockerComposeExecutable;
    }

    public Container get(String containerName) {
        containers.putIfAbsent(containerName, dockerComposeExecutable.container(containerName));
        return containers.get(containerName);
    }

}
