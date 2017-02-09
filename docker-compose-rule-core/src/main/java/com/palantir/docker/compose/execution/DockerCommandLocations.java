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

import static java.util.Arrays.asList;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public class DockerCommandLocations {
    private static final Predicate<String> IS_NOT_NULL = path -> path != null;
    private static final Predicate<String> FILE_EXISTS = path -> new File(path).exists();

    private final List<String> possiblePaths;
    private final String exeName;
    private final boolean lookInPath;

    public DockerCommandLocations(String exeName, boolean lookInPath, String... possiblePaths) {
        this.exeName = exeName;
        this.lookInPath = lookInPath;
        this.possiblePaths = asList(possiblePaths);
    }

    public Optional<String> preferredLocation() {
        List<String> envPath = (StringUtils.isNotBlank(exeName) && lookInPath)
                ? Arrays.asList(System.getenv("PATH").split(File.pathSeparator)) :
                Collections.emptyList();

        return Stream.concat(
                envPath.stream().map(path -> Paths.get(path, exeName).toAbsolutePath().toString()),
                possiblePaths.stream())
            .filter(IS_NOT_NULL)
            .filter(FILE_EXISTS)
            .findFirst();
    }

    @Override
    public String toString() {
        return "DockerCommandLocations{possiblePaths=" + possiblePaths + "}";
    }
}
