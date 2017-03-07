/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DockerCommandLocationsShould {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Rule public ExpectedException exception = ExpectedException.none();

    private Path firstPathFolder;
    private Path secondPathFolder;

    @Before
    public void setup() throws IOException {
        firstPathFolder = folder.newFolder("first").toPath();
        secondPathFolder = folder.newFolder("second").toPath();
    }

    @Test public void
    contain_the_contents_of_the_path_variable() {
        assumeTrue("Path variable present", System.getenv("PATH") != null);

        DockerCommandLocations commandLocations = DockerCommandLocations.builder()
                .macSearchLocations(Stream.empty())
                .build();
        Stream<Path> foundLocations = commandLocations.forCommand();

        String[] pathComponents = System.getenv("PATH").split(File.pathSeparator);
        List<Path> expectedPaths = Arrays.stream(pathComponents)
                .map(p -> Paths.get(p))
                .collect(toList());
        assertThat(foundLocations.collect(toList()), is(expectedPaths));
    }

    @Test public void
    contain_the_contents_of_the_path_with_a_single_folder() {
        DockerCommandLocations commandLocations = DockerCommandLocations.builder()
                .env(Collections.singletonMap("PATH", firstPathFolder.toString()))
                .macSearchLocations(Stream.empty())
                .build();

        assertThat(commandLocations.forCommand().collect(toList()), contains(firstPathFolder));
    }

    @Test public void
    contain_the_contents_of_the_path_with_two_folders() {
        DockerCommandLocations commandLocations = DockerCommandLocations.builder()
                .env(Collections.singletonMap("PATH", firstPathFolder.toString() + File.pathSeparator + secondPathFolder.toString()))
                .macSearchLocations(Stream.empty())
                .build();

        assertThat(commandLocations.forCommand().collect(toList()), contains(firstPathFolder, secondPathFolder));
    }

    @Test public void
    contain_the_location_override_before_the_contents_of_the_path() {
        DockerCommandLocations commandLocations = DockerCommandLocations.builder()
                .locationOverride(firstPathFolder.toString())
                .env(Collections.singletonMap("PATH", secondPathFolder.toString()))
                .macSearchLocations(Stream.empty())
                .build();

        assertThat(commandLocations.forCommand().collect(toList()), contains(firstPathFolder, secondPathFolder));
    }

    @Test public void
    contain_the_docker_for_mac_install_location_after_the_path() {
        DockerCommandLocations commandLocations = DockerCommandLocations.builder()
                .env(Collections.singletonMap("PATH", firstPathFolder.toString()))
                .macSearchLocations(Stream.of(secondPathFolder.toString()))
                .build();

        assertThat(commandLocations.forCommand().collect(toList()), contains(firstPathFolder, secondPathFolder));
    }

    @Test public void
    contain_the_contents_of_the_path_with_a_lowercase_environment_variable() {
        DockerCommandLocations commandLocations = DockerCommandLocations.builder()
                .env(Collections.singletonMap("path", firstPathFolder.toString()))
                .macSearchLocations(Stream.empty())
                .build();

        assertThat(commandLocations.forCommand().collect(toList()), contains(firstPathFolder));
    }

    @Test public void
    throw_an_exception_if_no_path_variable_is_present() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("No path environment variable found");

        DockerCommandLocations.builder()
            .env(Collections.emptyMap())
            .build();
    }

    @Test public void
    throw_an_exception_if_the_path_variable_is_empty() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Path variable was empty");

        DockerCommandLocations.builder()
            .env(Collections.singletonMap("PATH", ""))
            .build();
    }

}
