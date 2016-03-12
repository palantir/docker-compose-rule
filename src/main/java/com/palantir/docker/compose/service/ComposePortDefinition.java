/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import java.util.Optional;

public class ComposePortDefinition {

    private final int internalPort;
    private final Optional<Integer> externalPort;

    public ComposePortDefinition(int internalPort) {
        this.internalPort = internalPort;
        this.externalPort = Optional.empty();
    }

    public ComposePortDefinition(int externalPort, int internalPort) {
        this.internalPort = internalPort;
        this.externalPort = Optional.of(externalPort);
    }

    @Override
    public String toString() {
        if (externalPort.isPresent()) {
            return "\"" + externalPort.get() + ":" + internalPort + "\"";
        }
        return "\"" + internalPort + "\"";
    }

}
