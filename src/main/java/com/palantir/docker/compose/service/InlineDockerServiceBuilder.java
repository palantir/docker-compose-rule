/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import com.palantir.docker.compose.connection.waiting.HealthCheck;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;


public class InlineDockerServiceBuilder {

    private final String imageName;
    private final String serviceName;
    private ComposePortMappings ports = new ComposePortMappings();

    public InlineDockerServiceBuilder(String imageName, String serviceName) {
        this.imageName = imageName;
        this.serviceName = serviceName;
    }

    public InlineDockerServiceBuilder withPortMapping(int port) {
        ports = ports.withPort(new ComposePortDefinition(port));
        return this;
    }

    public InlineDockerServiceBuilder withPortMapping(int internalPort, int externalPort) {
        ports = ports.withPort(new ComposePortDefinition(internalPort, externalPort));
        return this;
    }

    public DockerService build() {
        File dockerComposeFile = buildDockerComposeFile();
        return DockerService.fromDockerCompositionFile(dockerComposeFile);
    }

    public DockerService withHealthCheck(HealthCheck healthCheck) {
        DockerService service = build();
        return service.withHealthCheck(serviceName, healthCheck);
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
                + "    image: " + imageName + "\n"
                + indent(ports.toString());
    }

    private String indent(String yaml) {
        return yaml.replaceAll("^", "    ").replaceAll("\n ", "\n     ");
    }

    private File createTemporaryDockerComposeFile() {
        try {
            return File.createTempFile("docker-compose-file-", ".yml");
        } catch (IOException e) {
            throw new RuntimeException("Error creating temporary docker compose file", e);
        }
    }

}
