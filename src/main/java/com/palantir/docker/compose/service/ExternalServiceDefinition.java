/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import java.io.File;
import java.util.Optional;

public class ExternalServiceDefinition implements ServiceDefinition {

    @Override
    public Optional<File> dockerComposeFileLocation() {
        return Optional.empty();
    }

}
