package com.palantir.docker.compose.configuration;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.UUID;

public class DockerComposeProjectName {
    private final String projectName = UUID.randomUUID().toString().substring(0, 8);

    public List<String> constructComposeFileCommand() {
        return ImmutableList.of("-p", projectName);
    }
}
