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

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import java.io.IOException;
import java.util.List;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class DockerComposeExecutable implements Executable {
    private static final Logger log = LoggerFactory.getLogger(DockerComposeExecutable.class);

    private static final DockerCommandLocations DOCKER_COMPOSE_LOCATIONS = new DockerCommandLocations(
            System.getenv("DOCKER_COMPOSE_LOCATION"),
            "/usr/local/bin/docker-compose",
            "/usr/bin/docker-compose"
    );

    private static String defaultDockerComposePath() {
        String pathToUse = DOCKER_COMPOSE_LOCATIONS.preferredLocation()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not find docker-compose, looked in: " + DOCKER_COMPOSE_LOCATIONS));

        log.debug("Using docker-compose found at " + pathToUse);

        return pathToUse;
    }

    @Value.Parameter protected abstract DockerComposeFiles dockerComposeFiles();
    @Value.Parameter protected abstract DockerConfiguration dockerConfiguration();

    @Value.Default public ProjectName projectName() {
        return ProjectName.random();
    }

    @Override
    public final String commandName() {
        return "docker-compose";
    }

    @Value.Derived
    protected String dockerComposePath() {
        return defaultDockerComposePath();
    }

    @Override
    public Process execute(String... commands) throws IOException {
        DockerForMacHostsIssue.issueWarning();

        List<String> args = ImmutableList.<String>builder()
                .add(dockerComposePath())
                .addAll(projectName().constructComposeFileCommand())
                .addAll(dockerComposeFiles().constructComposeFileCommand())
                .add(commands)
                .build();

        return dockerConfiguration().configuredDockerComposeProcess()
                .command(args)
                .redirectErrorStream(true)
                .start();
    }

    public static ImmutableDockerComposeExecutable.Builder builder() {
        return ImmutableDockerComposeExecutable.builder();
    }
}
