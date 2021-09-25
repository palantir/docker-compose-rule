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

package com.palantir.docker.compose.connection.waiting;

import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.failure;
import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.successful;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerPort;
import java.util.function.Function;
import org.assertj.core.api.HamcrestCondition;
import org.junit.Test;

public class HttpHealthCheckShould {
    private static final Function<DockerPort, String> URL_FUNCTION = _port -> null;
    public static final int PORT = 1234;
    private final Container container = mock(Container.class);

    @Test
    public void be_healthy_when_the_port_is_listening_over_http() {
        whenTheContainerIsListeningOnHttpTo(PORT, URL_FUNCTION);

        assertThat(HealthChecks.toRespondOverHttp(PORT, URL_FUNCTION).isHealthy(container)).is(new HamcrestCondition<>(is(successful())));
    }

    @Test
    public void be_unhealthy_when_all_ports_are_not_listening() {
        whenTheContainerIsNotListeningOnHttpTo(PORT, URL_FUNCTION);

        assertThat(HealthChecks.toRespondOverHttp(PORT, URL_FUNCTION).isHealthy(container)).is(new HamcrestCondition<>(is(failure())));
    }

    private void whenTheContainerIsListeningOnHttpTo(int port, Function<DockerPort, String> urlFunction) {
        when(container.portIsListeningOnHttp(port, urlFunction)).thenReturn(SuccessOrFailure.success());
    }

    private void whenTheContainerIsNotListeningOnHttpTo(int port, Function<DockerPort, String> urlFunction) {
        when(container.portIsListeningOnHttp(port, urlFunction)).thenReturn(SuccessOrFailure.failure("not listening"));
    }
}
