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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DockerCommandLocationsShould {
    private static final String badLocation = "file/that/does/not/exist";
    private static final String otherBadLocation = "another/file/that/does/not/exist";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private String goodLocation;

    @Before
    public void before() throws IOException {
        goodLocation = folder.newFile("docker-compose.yml").getAbsolutePath();
    }

    @Test
    public void provide_the_first_docker_command_location_if_it_exists() {
        DockerCommandLocations dockerCommandLocations =
                new DockerCommandLocations(badLocation, goodLocation, otherBadLocation);

        assertThat(dockerCommandLocations.preferredLocation().get()).isEqualTo(goodLocation);
    }

    @Test
    public void skip_paths_from_environment_variables_that_are_unset() {
        DockerCommandLocations dockerCommandLocations =
                new DockerCommandLocations(System.getenv("AN_UNSET_DOCKER_COMPOSE_PATH"), goodLocation);

        assertThat(dockerCommandLocations.preferredLocation().get()).isEqualTo(goodLocation);
    }

    @Test
    public void have_no_preferred_path_when_all_possible_paths_are_all_invalid() {
        DockerCommandLocations dockerCommandLocations = new DockerCommandLocations(badLocation);

        assertThat(dockerCommandLocations.preferredLocation()).isNotPresent();
    }
}
