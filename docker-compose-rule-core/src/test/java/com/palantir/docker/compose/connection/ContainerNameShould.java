/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class ContainerNameShould {

    @Test
    public void parse_container_name_correctly() {
        String customContainerName = "custom.container.name";
        assertThat(ContainerName.fromName(customContainerName), is(
                ImmutableContainerName.builder()
                        .rawName(customContainerName)
                        .semanticName(customContainerName)
                        .build()));
    }

    @Test
    public void parse_default_container_name_correctly() {
        String defaultContainerName = "directory_service_index";
        assertThat(ContainerName.fromName(defaultContainerName), is(
                ImmutableContainerName.builder()
                        .rawName(defaultContainerName)
                        .semanticName("service")
                        .build()));
    }

    @Test
    public void parse_default_container_name_with_slug_correctly() {
        String defaultContainerNameWithSlug = "directory_service_index_slug";
        assertThat(ContainerName.fromName(defaultContainerNameWithSlug), is(
                ImmutableContainerName.builder()
                        .rawName(defaultContainerNameWithSlug)
                        .semanticName("service")
                        .build()));
    }
}
