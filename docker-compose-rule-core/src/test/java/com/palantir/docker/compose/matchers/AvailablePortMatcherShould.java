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
package com.palantir.docker.compose.matchers;

import static com.google.common.collect.Lists.newArrayList;
import static com.palantir.docker.compose.matchers.AvailablePortMatcher.areAvailable;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.docker.compose.connection.DockerPort;
import java.util.List;
import org.assertj.core.api.HamcrestCondition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AvailablePortMatcherShould {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void succeed_when_there_are_no_unavailable_ports() {
        List<DockerPort> unavailablePorts = emptyList();
        assertThat(unavailablePorts).is(new HamcrestCondition<>(areAvailable()));
    }

    @Test
    public void throw_exception_when_there_are_some_unavailable_ports() {
        List<DockerPort> unavailablePorts =
                newArrayList(new DockerPort("0.0.0.0", 1234, 1234), new DockerPort("1.2.3.4", 2345, 3456));
        exception.expect(AssertionError.class);
        exception.expectMessage("For host with ip address: 0.0.0.0");
        exception.expectMessage("external port '1234' mapped to internal port '1234' was unavailable");
        exception.expectMessage("For host with ip address: 1.2.3.4");
        exception.expectMessage("external port '2345' mapped to internal port '3456' was unavailable");
        assertThat(unavailablePorts).is(new HamcrestCondition<>(areAvailable()));
    }
}
