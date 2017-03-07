/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.docker.compose.execution;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DockerCommandLocator {
    private static final Pattern PATH_SPLITTER = Pattern.compile(File.pathSeparator);

    protected abstract String command();

    @Value.Default
    protected boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    @Value.Derived
    protected String executableName() {
        if (isWindows()) {
            return command() + ".exe";
        }
        return command();
    }

    @Value.Default
    protected String path() {
        String path = System.getenv("PATH");
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

    @Value.Derived
    protected Stream<String> searchLocations() {
        Stream<String> pathLocations = Stream.concat(PATH_SPLITTER.splitAsStream(path()), macSearchLocations());
        if (locationOverride() == null) {
            return pathLocations;
        }
        return Stream.concat(Stream.of(locationOverride()), pathLocations);
    }

    public String getLocation() {
        return searchLocations()
                .map(p -> Paths.get(p, executableName()))
                .filter(Files::exists)
                .findFirst()
                .map(Path::toString)
                .orElseThrow(() -> new IllegalStateException("Could not find " + command() + " in path"));
    }

    public static ImmutableDockerCommandLocator.Builder forCommand(String command) {
        return ImmutableDockerCommandLocator.builder()
                .command(command);
    }

}
