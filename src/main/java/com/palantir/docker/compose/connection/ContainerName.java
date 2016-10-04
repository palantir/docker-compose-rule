/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ContainerName {

    @Value.Parameter
    public abstract String semanticName();

    @Override
    public String toString() {
        return semanticName();
    }

    public static ContainerName fromPsLine(String psLine) {
        String[] lineComponents = psLine.split(" ");
        String rawName = lineComponents[0];
        String semanticName = withoutDirectory(withoutScaleNumber(rawName));
        return ImmutableContainerName.of(semanticName);
    }

    private static String withoutDirectory(String rawName) {
        return Arrays.stream(rawName.split("_"))
                .skip(1)
                .collect(joining("_"));
    }

    public static String withoutScaleNumber(String rawName) {
        String[] components = rawName.split("_");
        return Arrays.stream(components)
                .limit(components.length - 1)
                .collect(joining("_"));
    }

}
