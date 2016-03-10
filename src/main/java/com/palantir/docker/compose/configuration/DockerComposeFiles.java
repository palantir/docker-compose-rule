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
package com.palantir.docker.compose.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class DockerComposeFiles {

    private final List<File> dockerComposeFiles;

    public DockerComposeFiles(List<File> dockerComposeFiles) {
        this.dockerComposeFiles = dockerComposeFiles;
        validateComposeFilesExist(dockerComposeFiles);
    }

    public static DockerComposeFiles from(String... dockerComposeFilenames) {
        List<File> dockerComposeFiles = newArrayList(dockerComposeFilenames).stream()
                .map(File::new)
                .collect(toList());
        validateAtLeastOneComposeFileSpecified(dockerComposeFiles);
        return new DockerComposeFiles(dockerComposeFiles);
    }

    public DockerComposeFiles withAdditionalFile(File composeFile) {
        List<File> combinedFiles = new ArrayList<>(dockerComposeFiles);
        combinedFiles.add(composeFile);
        return new DockerComposeFiles(combinedFiles);
    }

    public List<String> constructComposeFileCommand() {
        return dockerComposeFiles.stream()
                .map(File::getAbsolutePath)
                .map(f -> newArrayList("--file", f))
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private static void validateAtLeastOneComposeFileSpecified(List<File> dockerComposeFiles) {
        checkArgument(!dockerComposeFiles.isEmpty(), "A docker compose file must be specified.");
    }

    private static void validateComposeFilesExist(List<File> dockerComposeFiles) {
        List<File> missingFiles = dockerComposeFiles.stream()
                                                    .filter(f -> !f.exists())
                                                    .collect(toList());

        String errorMessage = missingFiles.stream()
                .map(File::getAbsolutePath)
                .collect(joining(", ", "The following docker-compose files: ", " do not exist."));
        checkState(missingFiles.isEmpty(), errorMessage);
    }

}
