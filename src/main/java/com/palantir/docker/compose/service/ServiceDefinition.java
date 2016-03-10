/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import java.io.File;
import java.util.Optional;

public interface ServiceDefinition {

    Optional<File> dockerComposeFileLocation();

    static ServiceDefinition fromFile(String dockerComposeFile) {
        return new DockerComposeFileBasedServiceDefinition(dockerComposeFile);
    }

    static ServiceDefinition external() {
        return new ExternalServiceDefinition();
    }

}
