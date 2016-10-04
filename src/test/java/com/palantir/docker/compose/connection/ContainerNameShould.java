/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.List;
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

    @Test
    public void can_handle_custom_container_names() {
        ContainerName name = ContainerName.fromPsLine("test-1.container.name   /docker-entrypoint.sh postgres   Up      5432/tcp");

        ContainerName expected = ImmutableContainerName.builder()
                .rawName("test-1.container.name")
                .semanticName("test-1.container.name")
                .build();

        assertThat(name, is(expected));
    }

    @Test
    public void result_in_no_container_names_when_ps_output_is_empty() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\n");
        assertThat(names, is(emptyList()));
    }

    @Test
    public void result_in_a_single_container_name_when_ps_output_has_a_single_container() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents");
        assertThat(names, contains(containerName("dir", "db", "1")));
    }

    @Test
    public void allow_containers_with_underscores_in_their_name() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_left_right_1 other line contents");
        assertThat(names, contains(containerName("dir", "left_right", "1")));
    }

    @Test
    public void result_in_two_container_names_when_ps_output_has_two_containers() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\ndir_db2_1 other stuff");
        assertThat(names, contains(containerName("dir", "db", "1"), containerName("dir", "db2", "1")));
    }

    @Test
    public void ignore_an_empty_line_in_ps_output() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n\n");
        assertThat(names, contains(containerName("dir", "db", "1")));
    }

    @Test
    public void ignore_a_line_with_ony_spaces_in_ps_output() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n   \n");
        assertThat(names, contains(containerName("dir", "db", "1")));
    }

    private static ContainerName containerName(String project, String semantic, String number) {
        return ImmutableContainerName.builder()
                .rawName(project + "_" + semantic + "_" + number)
                .semanticName(semantic)
                .build();
    }
}
