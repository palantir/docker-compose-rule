/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class DockerCommandLocations {
    private static final Predicate<String> IS_NOT_NULL = path -> path != null;
    private static final Predicate<String> FILE_EXISTS = path -> new File(path).exists();

    private final List<String> possiblePaths;

    public DockerCommandLocations(String... possiblePaths) {
        this.possiblePaths = asList(possiblePaths);
    }

    public Optional<String> preferredLocation() {

        return possiblePaths.stream()
                .filter(IS_NOT_NULL)
                .filter(FILE_EXISTS)
                .findFirst();
    }

    @Override
    public String toString() {
        return "DockerCommandLocations{possiblePaths=" + possiblePaths + "}";
    }
}
