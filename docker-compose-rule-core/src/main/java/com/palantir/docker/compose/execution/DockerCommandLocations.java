/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DockerCommandLocations {

    private static final Pattern PATH_SPLITTER = Pattern.compile(File.pathSeparator);

    @Value.Default
    protected Map<String, String> env() {
        return System.getenv();
    }

    @Value.Derived
    protected String path() {
        String path = env().get("PATH");
        if (path == null) {
            path = env().get("path");
        }
        if (path == null) {
            throw new IllegalStateException("No path environment variable found");
        }
        return path;
    }

    @Value.Check
    protected void pathIsNotEmpty() {
        if (path().isEmpty()) {
            throw new IllegalStateException("Path variable was empty");
        }
    }

    @Nullable
    protected abstract String locationOverride();

    @Value.Default
    protected Stream<String> macSearchLocations() {
        return Stream.of("/usr/local/bin", "/usr/bin");
    }

    private Stream<String> pathLocations() {
        Stream<String> pathLocations = Stream.concat(PATH_SPLITTER.splitAsStream(path()), macSearchLocations());
        if (locationOverride() == null) {
            return pathLocations;
        }
        return Stream.concat(Stream.of(locationOverride()), pathLocations);
    }

    public Stream<Path> forCommand() {
        return pathLocations().map(p -> Paths.get(p));
    }

    public static DockerCommandLocations withOverride(String override) {
        return ImmutableDockerCommandLocations.builder()
                .locationOverride(override)
                .build();
    }

    public static ImmutableDockerCommandLocations.Builder builder() {
        return ImmutableDockerCommandLocations.builder();
    }

}
