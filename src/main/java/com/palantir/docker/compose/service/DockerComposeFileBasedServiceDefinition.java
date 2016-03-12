/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import java.io.File;
import java.util.Optional;

public class DockerComposeFileBasedServiceDefinition implements ServiceDefinition {

    private final File dockerComposeFile;

    public DockerComposeFileBasedServiceDefinition(File dockerComposeFile) {
        this.dockerComposeFile = dockerComposeFile;
    }

    @Override
    public Optional<File> dockerComposeFileLocation() {
        return Optional.of(dockerComposeFile);
    }

}
