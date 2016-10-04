/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ContainerNameShould {
    @Test
    public void parse_a_semantic_name_correctly_from_a_single_line() {
        ContainerName names = ContainerName.fromPsLine("dir_db_1 other line contents");
        assertThat(names, is(ImmutableContainerName.of("db")));
    }

}
