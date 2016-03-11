/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;


public class InlineDockerServiceBuilder {

    private final String imageName;
    private final String serviceName;

    public InlineDockerServiceBuilder(String imageName, String serviceName) {
        this.imageName = imageName;
        this.serviceName = serviceName;
    }

    public InlineDockerServiceBuilder withPortMapping(String string) {
        return null;
    }

    public DockerService build() {
        File dockerComposeFile = buildDockerComposeFile();
        return DockerService.fromDockerCompositionFile(dockerComposeFile);
    }

    private File buildDockerComposeFile() {
        File file = createTemporaryDockerComposeFile();
        try {
            FileUtils.write(file, buildDockerComposeFileContents());
        } catch (IOException e) {
            throw new RuntimeException("Error writing docker compose file contents", e);
        }
        return file;
    }

    private String buildDockerComposeFileContents() {
        return serviceName + ":\n"
                + "    image: " + imageName;
    }

    private File createTemporaryDockerComposeFile() {
        try {
            return File.createTempFile("docker-compose-file-", ".yml");
        } catch (IOException e) {
            throw new RuntimeException("Error creating temporary docker compose file", e);
        }
    }

}
