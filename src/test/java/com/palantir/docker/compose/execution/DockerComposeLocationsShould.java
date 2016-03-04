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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DockerComposeLocationsShould {
    private final String badLocation = "file/that/does/not/exist";
    private final String otherBadLocation = "another/file/that/does/not/exist";

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    String goodLocation;

    @Before
    public void setup() throws IOException {
        goodLocation = folder.newFile("docker-compose.yml").getAbsolutePath();
    }

    @Test public void
    provide_the_first_docker_compose_location_if_it_exists() throws IOException {
        DockerComposeLocations dockerComposeLocations = new DockerComposeLocations(
                badLocation,
                goodLocation,
                otherBadLocation);

        assertThat(dockerComposeLocations.preferredLocation().get(),
                is(goodLocation));
    }

    @Test public void
    skip_paths_from_environment_variables_that_are_unset() {
        DockerComposeLocations dockerComposeLocations = new DockerComposeLocations(
                System.getenv("AN_UNSET_DOCKER_COMPOSE_PATH"),
                goodLocation);

        assertThat(dockerComposeLocations.preferredLocation().get(),
                is(goodLocation));
    }

    @Test public void
    have_no_preferred_path_when_all_possible_paths_are_all_invalid() {
        DockerComposeLocations dockerComposeLocations = new DockerComposeLocations(
                badLocation);

        assertThat(dockerComposeLocations.preferredLocation(),
                is(empty()));
    }
}
