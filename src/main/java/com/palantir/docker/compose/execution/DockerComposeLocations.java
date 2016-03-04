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
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

public class DockerComposeLocations {
    private final List<String> paths;

    public DockerComposeLocations(String... paths) {
        this.paths = asList(paths);
    }

    public Optional<String> preferredLocation() {
        return paths.stream()
                .filter(path -> path != null)
                .filter(path -> new File(path).exists())
                .findFirst();
    }
}
