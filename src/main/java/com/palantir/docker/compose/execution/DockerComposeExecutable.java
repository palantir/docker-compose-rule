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

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;


public class DockerComposeExecutable {
    private static final Logger log = LoggerFactory.getLogger(DockerComposeExecutable.class);

    private static final DockerComposeLocations DOCKER_COMPOSE_LOCATIONS = new DockerComposeLocations(
            System.getenv("DOCKER_COMPOSE_LOCATION"),
            "/usr/local/bin/docker-compose"
    );

    private final DockerComposeFiles dockerComposeFiles;
    private final DockerConfiguration dockerConfiguration;
    private final String dockerComposePath;

    public DockerComposeExecutable(DockerComposeFiles dockerComposeFiles, DockerConfiguration dockerConfiguration) {
        this.dockerComposePath = findDockerComposePath();
        this.dockerComposeFiles = dockerComposeFiles;
        this.dockerConfiguration = dockerConfiguration;
    }

    public Process execute(String... commands) throws IOException {
        List<String> args = ImmutableList.<String>builder()
                .add(dockerComposePath)
                .addAll(dockerComposeFiles.constructComposeFileCommand())
                .add(commands)
                .build();

        return dockerConfiguration.configuredDockerComposeProcess()
                .command(args)
                .redirectErrorStream(true)
                .start();
    }

    public DockerComposeFiles dockerComposeFiles() {
        return dockerComposeFiles;
    }

    private static String findDockerComposePath() {
        String pathToUse = DOCKER_COMPOSE_LOCATIONS.preferredLocation()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not find docker-compose, looked in: " + DOCKER_COMPOSE_LOCATIONS));

        log.debug("Using docker-compose found at " + pathToUse);

        return pathToUse;
    }

}
