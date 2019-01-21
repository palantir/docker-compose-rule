/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class DockerPortFormattingShould {
    private final DockerPort dockerPort = new DockerPort("hostname", 1234, 4321);

    @Test public void
    have_no_effect_on_a_string_with_no_substitutions() {
        assertThat(
                dockerPort.inFormat("no substitutions"),
                is("no substitutions"));
    }

    @Test public void
    allow_building_an_externally_accessible_address() {
        assertThat(
                dockerPort.inFormat("http://$HOST:$EXTERNAL_PORT/api"),
                is("http://hostname:1234/api"));
    }

    @Test public void
    allow_building_an_address_with_an_internal_port() {
        assertThat(
                dockerPort.inFormat("http://localhost:$INTERNAL_PORT/api"),
                is("http://localhost:4321/api"));
    }

    @Test public void
    allow_multiple_copies_of_each_substitution() {
        assertThat(
                dockerPort.inFormat("$HOST,$HOST,$INTERNAL_PORT,$INTERNAL_PORT,$EXTERNAL_PORT,$EXTERNAL_PORT"),
                is("hostname,hostname,4321,4321,1234,1234"));
    }

}
