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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import org.junit.Test;

public class ContainerNamesShould {

    @Test
    public void result_in_no_container_names_when_ps_output_is_empty() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\n");
        assertThat(names, is(containerNames()));
    }

    @Test
    public void result_in_a_single_container_name_when_ps_output_has_a_single_container() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents");
        assertThat(names, is(containerNames("db")));
    }

    @Test
    public void allow_containers_with_underscores_in_their_name() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_left_right_1 other line contents");
        assertThat(names, is(containerNames("left_right")));
    }

    @Test
    public void result_in_two_container_names_when_ps_output_has_two_containers() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\ndir_db2_1 other stuff");
        assertThat(names, is(containerNames("db", "db2")));
    }

    @Test
    public void ignore_an_empty_line_in_ps_output() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n\n");
        assertThat(names, is(containerNames("db")));
    }

    @Test
    public void ignore_a_line_with_ony_spaces_in_ps_output() {
        ContainerNames names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n   \n");
        assertThat(names, is(containerNames("db")));
    }

    public static ContainerNames containerNames(String... names) {
        return new ContainerNames(Arrays.asList(names));
    }
}
