/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class ContainerNameShould {

    @Test
    public void parse_semantic_and_raw_names_correctly_from_container_names() {
        String customContainerName = "custom.container.name";
        assertThat(ContainerName.fromName(customContainerName), is(
                ImmutableContainerName.builder()
                .rawName(customContainerName)
                .semanticName(customContainerName)
                .build()));

        String defaultContainerName = "directory_service_index";
        assertThat(ContainerName.fromName(defaultContainerName), is(
                ImmutableContainerName.builder()
                        .rawName(defaultContainerName)
                        .semanticName("service")
                        .build()));

        String defaultContainerNameWithSlug = "directory_service_index_slug";
        assertThat(ContainerName.fromName(defaultContainerNameWithSlug), is(
                ImmutableContainerName.builder()
                        .rawName(defaultContainerNameWithSlug)
                        .semanticName("service")
                        .build()));
    }

    /*
    @Test
    public void allow_windows_newline_characters() {
        List<ContainerName> names = ContainerNames.parseFromPortsString("\r\n----\r\ndir_db_1 other line contents");
        assertThat(names, contains(containerName("dir", "db", "1")));
    }

    @Test
    public void allow_containers_with_underscores_in_their_name() {
        List<ContainerName> names = ContainerNames.parseFromPortsString("\n----\ndir_left_right_1 other line contents");
        assertThat(names, contains(containerName("dir", "left_right", "1")));
    }
    */
}
