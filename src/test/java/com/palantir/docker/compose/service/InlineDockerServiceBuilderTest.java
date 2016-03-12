/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.service;

import com.palantir.docker.compose.matchers.IOMatchers;
import org.junit.Test;

import java.io.File;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class InlineDockerServiceBuilderTest {

    @Test
    public void inline_service_generates_a_docker_compose_file_with_service_name_and_image() {
        InlineDockerServiceBuilder builder = new InlineDockerServiceBuilder("imageName", "service");
        Optional<File> dockerComposeFile = builder.build().dockerComposeFileLocation();

        assertThat(dockerComposeFile.isPresent(), is(true));
        assertThat(dockerComposeFile.get(), is(IOMatchers.fileContainingString(
            "service:\n" +
            "    image: imageName\n"
        )));
    }

    @Test
    public void inline_service_generates_a_docker_compose_file_with_a_single_port_exposed() {
        InlineDockerServiceBuilder builder = new InlineDockerServiceBuilder("imageName", "service")
            .withPortMapping(1234);
        Optional<File> dockerComposeFile = builder.build().dockerComposeFileLocation();

        assertThat(dockerComposeFile.isPresent(), is(true));
        assertThat(dockerComposeFile.get(), is(IOMatchers.fileContainingString(
            "service:\n" +
            "    image: imageName\n" +
            "    ports:\n" +
            "        - \"1234\"\n"
        )));
    }

    @Test
    public void inline_service_generates_a_docker_compose_file_with_an_internal_port_and_an_external_port() {
        InlineDockerServiceBuilder builder = new InlineDockerServiceBuilder("imageName", "service")
            .withPortMapping(1234)
            .withPortMapping(1235, 1234);
        Optional<File> dockerComposeFile = builder.build().dockerComposeFileLocation();

        assertThat(dockerComposeFile.isPresent(), is(true));
        assertThat(dockerComposeFile.get(), is(IOMatchers.fileContainingString(
            "service:\n" +
            "    image: imageName\n" +
            "    ports:\n" +
            "        - \"1234\"\n" +
            "        - \"1235:1234\"\n"
        )));
    }


}
