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

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ContainerNamesTest {

    @Test
    public void empty_ps_output_results_in_no_container_names() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\n");
        assertThat(names, is(new ContainerNames(emptyList())));
    }

    @Test
    public void ps_output_with_a_single_container_results_in_a_single_container_name() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents");
        assertThat(names, is(new ContainerNames("db")));
    }

    @Test
    public void containers_with_underscores_in_their_name_are_allowed() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_left_right_1 other line contents");
        assertThat(names, is(new ContainerNames("left_right")));
    }

    @Test
    public void ps_output_with_two_containers_results_in_a_two_container_name() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\ndir_db2_1 other stuff");
        assertThat(names, is(new ContainerNames(asList("db", "db2"))));
    }

    @Test
    public void an_empty_line_in_ps_output_is_ignored() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n\n");
        assertThat(names, is(new ContainerNames("db")));
    }

    @Test
    public void a_line_with_ony_spaces_in_ps_output_is_ignored() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n   \n");
        assertThat(names, is(new ContainerNames("db")));
    }

}
