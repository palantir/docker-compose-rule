/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.execution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import java.io.File;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;

public class DockerCommandLocationsShould {

    @Rule public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule public ExpectedException exception = ExpectedException.none();

    @Test public void
    contain_the_contents_of_the_path_with_a_single_folder() {
        environmentVariables.set("PATH", "/folder");

        assertThat(DockerCommandLocations.pathLocations(), hasItem(Paths.get("/folder")));
    }

    @Test public void
    contain_the_contents_of_the_path_with_two_folders() {
        environmentVariables.set("PATH", "/first" + File.pathSeparator + "/second");

        assertThat(DockerCommandLocations.pathLocations(), hasItems(Paths.get("/first"), Paths.get("/second")));
    }

    @Test public void
    contain_the_docker_for_mac_install_location_after_the_path() {
        environmentVariables.set("PATH", "/folder");

        assertThat(DockerCommandLocations.pathLocations(), contains(Paths.get("/folder"), Paths.get("/usr/local/bin"), Paths.get("/usr/bin")));
    }

    @Test public void
    contain_the_contents_of_the_path_with_a_lowercase_environment_variable() {
        environmentVariables.set("PATH", null);
        environmentVariables.set("path", "/lowercase");

        assertThat(DockerCommandLocations.pathLocations(), hasItem(Paths.get("/lowercase")));
    }

    @Test public void
    return_just_the_mac_locations_if_no_path_variable_is_present() {
        environmentVariables.set("PATH", null);

        assertThat(DockerCommandLocations.pathLocations(), contains(Paths.get("/usr/local/bin"), Paths.get("/usr/bin")));
    }

}
