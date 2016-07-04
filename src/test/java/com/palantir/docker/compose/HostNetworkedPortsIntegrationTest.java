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
package com.palantir.docker.compose;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import org.junit.Rule;
import org.junit.Test;

public class HostNetworkedPortsIntegrationTest {
    @Rule
    public DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/test/resources/host-networked-docker-compose.yaml")
            .waitingForHostNetworkedPort(5432, toBeOpen())
            .build();

    private HealthCheck<DockerPort> toBeOpen() {
        return port -> SuccessOrFailure.fromBoolean(port.isListeningNow(), "" + port + "was not listening");
    }

    @Test public void
    can_access_host_networked_ports() {
        assertThat(docker.hostNetworkedPort(5432).getInternalPort(), is(5432));
        assertThat(docker.hostNetworkedPort(5432).getExternalPort(), is(5432));
    }
}
