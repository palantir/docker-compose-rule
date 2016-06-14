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

import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.execution.DockerCompose;
import org.junit.Before;
import org.junit.Test;

public class ContainerCacheShould {

    private static final String CONTAINER_NAME = "container";

    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final ContainerCache containers = new ContainerCache(dockerCompose);

    @Before
    public void setup() {
        when(dockerCompose.container(CONTAINER_NAME)).thenReturn(new Container(CONTAINER_NAME, dockerCompose));
    }

    @Test
    public void return_a_container_with_the_specified_name_when_getting_a_new_container() {
        Container container = containers.container(CONTAINER_NAME);
        assertThat(container, is(new Container(CONTAINER_NAME, dockerCompose)));
    }

    @Test
    public void return_the_same_object_when_getting_a_container_twice() {
        Container container = containers.container(CONTAINER_NAME);
        Container sameContainer = containers.container(CONTAINER_NAME);
        assertThat(container, is(sameInstance(sameContainer)));
    }

}
