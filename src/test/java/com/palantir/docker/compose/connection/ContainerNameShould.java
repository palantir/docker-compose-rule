/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ContainerNameShould {

    @Test
    public void parse_a_semantic_and_raw_name_correctly_from_a_single_line() {
        ContainerName actual = ContainerName.fromPsLine("dir_db_1 other line contents");

        ContainerName expected = ImmutableContainerName.builder()
                .rawName("dir_db_1")
                .semanticName("db")
                .build();

        assertThat(actual, is(expected));
    }

}
