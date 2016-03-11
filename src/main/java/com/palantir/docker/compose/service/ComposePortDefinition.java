/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

public class ComposePortDefinition {

    private final int internalPort;

    public ComposePortDefinition(int internalPort) {
        this.internalPort = internalPort;
    }

    @Override
    public String toString() {
        return "\"" + internalPort + "\"";
    }

}
