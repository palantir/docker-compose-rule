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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.immutables.value.Value;

@Value.Immutable
public abstract class DockerCommandLocations {
    private static final Predicate<String> IS_NOT_NULL = path -> path != null;
    private static final Predicate<String> FILE_EXISTS = path -> new File(path).exists();

    protected abstract String executableName();
    protected abstract List<Optional<String>> additionalSearchLocations();
    @Value.Default protected List<String> pathLocations() {
        String envpath = System.getenv("PATH");
        return envpath == null ? Collections.emptyList() : Arrays.asList(envpath.split(File.pathSeparator));
    }

    @Value.Derived
    public Optional<String> preferredLocation() {
        return Stream.concat(
                pathLocations().stream().map(path -> Paths.get(path, executableName()).toAbsolutePath().toString()),
                additionalSearchLocations().stream().filter(Optional::isPresent).map(Optional::get))
            .filter(IS_NOT_NULL)
            .filter(FILE_EXISTS)
            .findFirst();
    }

    @Override
    public String toString() {
        return "DockerCommandLocations{additionalSearchLocations=" + additionalSearchLocations() + "}";
    }

    public static ImmutableDockerCommandLocations.Builder builder() {
        return ImmutableDockerCommandLocations.builder();
    }

    /**
     * convert an array of strings to a list of optionals. used to make calling `additionalSearchLocations' a bit friendlier.
     */
    public static List<Optional<String>> optionals(String ... strings) {
        return Arrays.stream(strings).map(Optional::ofNullable).collect(Collectors.toList());
    }
}
