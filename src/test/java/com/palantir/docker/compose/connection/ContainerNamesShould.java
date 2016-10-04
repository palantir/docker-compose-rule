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
package com.palantir.docker.compose.connection;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import org.junit.Test;

public class ContainerNamesShould {

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

    public static ContainerName containerName(String project, String semantic, String number) {
        return ImmutableContainerName.builder()
                .rawName(project + "_" + semantic + "_" + number)
                .semanticName(semantic)
                .build();
    }
}
