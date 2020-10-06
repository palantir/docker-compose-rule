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
import java.io.IOException;
import java.util.List;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class DockerExecutable implements Executable {
    private static final Logger log = LoggerFactory.getLogger(DockerExecutable.class);

    private static final DockerCommandLocations DOCKER_LOCATIONS = new DockerCommandLocations(
            System.getenv("DOCKER_LOCATION"),
            "/usr/local/bin/docker",
            "/usr/bin/docker",
            "C:/program files/docker/docker/resources/bin/docker.exe"
    );

    @Value.Parameter protected abstract DockerConfiguration dockerConfiguration();

    @Override
    public final String commandName() {
        return "docker";
    }

    @Value.Derived
    protected String dockerPath() {
        String pathToUse = DOCKER_LOCATIONS.preferredLocation()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not find docker, looked in: " + DOCKER_LOCATIONS));

        log.debug("Using docker found at " + pathToUse);

        return pathToUse;
    }

    @Override
    public Process execute(String... commands) throws IOException {
        List<String> args = ImmutableList.<String>builder()
                .add(dockerPath())
                .add(commands)
                .build();

        return dockerConfiguration().configuredDockerComposeProcess()
                .command(args)
                .redirectErrorStream(true)
                .start();
    }

    public static ImmutableDockerExecutable.Builder builder() {
        return ImmutableDockerExecutable.builder();
    }
}
