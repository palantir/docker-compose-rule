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
package com.palantir.docker.compose.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DockerComposeFiles {

    private final List<File> dockerComposeFiles;

    public DockerComposeFiles(List<File> dockerComposeFiles) {
        this.dockerComposeFiles = dockerComposeFiles;
    }

    public static DockerComposeFiles from(String... dockerComposeFilenames) {
        List<File> dockerComposeFiles = Lists.newArrayList(dockerComposeFilenames).stream()
                .map(File::new)
                .collect(Collectors.toList());
        validateAtLeastOneComposeFileSpecified(dockerComposeFiles);
        validateComposeFilesExist(dockerComposeFiles);
        return new DockerComposeFiles(dockerComposeFiles);
    }

    public List<String> constructComposeFileCommand() {
        return dockerComposeFiles.stream()
                .map(File::getAbsolutePath)
                .map(f -> Lists.newArrayList("--file", f))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static void validateAtLeastOneComposeFileSpecified(List<File> dockerComposeFiles) {
        Preconditions.checkArgument(!dockerComposeFiles.isEmpty(), "A docker compose file must be specified.");
    }

    private static void validateComposeFilesExist(List<File> dockerComposeFiles) {
        List<File> missingFiles =
                dockerComposeFiles.stream().filter(f -> !f.exists()).collect(Collectors.toList());

        String errorMessage = missingFiles.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(", ", "The following docker-compose files: ", " do not exist."));
        Preconditions.checkState(missingFiles.isEmpty(), errorMessage);
    }
}
