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
package com.palantir.docker.compose.connection.waiting;

import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.failure;
import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.successful;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.connection.Container;
import org.junit.Test;

public class PortsHealthCheckShould {
    private final SingleServiceHealthCheck healthCheck = HealthChecks.toHaveAllPortsOpen();
    private final Container container = mock(Container.class);

    @Test
    public void be_healthy_when_all_ports_are_listening() {
        whenTheContainerHasAllPortsOpen();

        assertThat(healthCheck.isServiceUp(container), is(successful()));
    }

    @Test
    public void be_unhealthy_when_all_ports_are_not_listening() {
        whenTheContainerDoesNotHaveAllPortsOpen();

        assertThat(healthCheck.isServiceUp(container), is(failure()));
    }

    private void whenTheContainerDoesNotHaveAllPortsOpen() {
        when(container.areAllPortsOpen()).thenReturn(SuccessOrFailure.failure("not all ports open"));
    }

    private void whenTheContainerHasAllPortsOpen() {
        when(container.areAllPortsOpen()).thenReturn(SuccessOrFailure.success());
    }
}
