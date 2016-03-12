/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

public class ComposePortMappings {

    private final List<ComposePortDefinition> ports;

    public ComposePortMappings() {
        ports = Collections.emptyList();
    }

    private ComposePortMappings(List<ComposePortDefinition> ports) {
        this.ports = ports;
    }

    public ComposePortMappings withPort(ComposePortDefinition composePortDefinition) {
        ImmutableList<ComposePortDefinition> newPorts = ImmutableList.<ComposePortDefinition>builder()
                .addAll(ports)
                .add(composePortDefinition)
                .build();
        return new ComposePortMappings(newPorts);
    }

    @Override
    public String toString() {
        if (ports.isEmpty()) {
            return "";
        }
        StringBuilder portString = new StringBuilder("ports:\n");
        for (ComposePortDefinition port : ports) {
            portString.append("    - " + port.toString() + "\n");
        }
        return portString.toString();
    }
}
