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

import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import java.io.IOException;
import java.util.List;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
@SuppressWarnings("DesignForExtension")
public abstract class DockerComposeExecutable implements Executable {
    private static final Logger log = LoggerFactory.getLogger(DockerComposeExecutable.class);

    private static final DockerCommandLocations DOCKER_COMPOSE_V1_LOCATIONS = new DockerCommandLocations(
            System.getenv("DOCKER_COMPOSE_LOCATION"), "/usr/local/bin/docker-compose", "/usr/bin/docker-compose");

    private static final DockerCommandLocations DOCKER_COMPOSE_V2_LOCATIONS = new DockerCommandLocations(
            System.getenv("DOCKER_COMPOSE_LOCATION"), "/usr/local/bin/docker", "/usr/bin/docker");

    private static String defaultDockerComposePath(boolean useDockerComposeV2) {
        DockerCommandLocations locations =
                useDockerComposeV2 ? DOCKER_COMPOSE_V2_LOCATIONS : DOCKER_COMPOSE_V1_LOCATIONS;
        String pathToUse = locations
                .preferredLocation()
                .orElseThrow(() -> new IllegalStateException("Could not find docker-compose, looked in: " + locations));

        log.debug("Using docker-compose found at " + pathToUse);

        return pathToUse;
    }

    /**
     * Returns the version of docker-compose.
     *
     * @deprecated only supports getting the version of docker-compose v1.
     * Just use {@link #execute} to get the version directly.
     */
    @Deprecated
    static Version version() throws IOException, InterruptedException {
        Command dockerCompose = new Command(
                new Executable() {
                    @Override
                    public String commandName() {
                        return "docker-compose";
                    }

                    @Override
                    public Process execute(String... commands) throws IOException {
                        List<String> args = ImmutableList.<String>builder()
                                .add(defaultDockerComposePath(false))
                                .add(commands)
                                .build();
                        return new ProcessBuilder(args)
                                .redirectErrorStream(true)
                                .start();
                    }
                },
                log::trace);

        String versionOutput = dockerCompose.execute(Command.throwingOnError(), "-v");
        return DockerComposeVersion.parseFromDockerComposeVersion(versionOutput);
    }

    @Value.Parameter
    protected abstract DockerComposeFiles dockerComposeFiles();

    @Value.Parameter
    protected abstract DockerConfiguration dockerConfiguration();

    @Value.Default
    public ProjectName projectName() {
        return ProjectName.random();
    }

    @Override
    public final String commandName() {
        return useDockerComposeV2() ? "docker compose" : "docker-compose";
    }

    @Value.Derived
    protected String dockerComposePath() {
        return defaultDockerComposePath(useDockerComposeV2());
    }

    @Value.Default
    public boolean useDockerComposeV2() {
        return false;
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

        return dockerConfiguration()
                .configuredDockerComposeProcess()
                .command(args)
                .redirectErrorStream(true)
                .start();
    }

    public static ImmutableDockerComposeExecutable.Builder builder() {
        return ImmutableDockerComposeExecutable.builder();
    }
}
