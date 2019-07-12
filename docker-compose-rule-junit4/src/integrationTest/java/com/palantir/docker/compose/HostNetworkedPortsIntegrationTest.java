/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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
import static org.junit.Assume.assumeTrue;

import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Ignore;
import org.junit.Test;

public class HostNetworkedPortsIntegrationTest {
    private static HealthCheck<DockerPort> toBeOpen() {
        return port -> SuccessOrFailure.fromBoolean(port.isListeningNow(), "Internal port " + port + " was not listening");
    }

    @Ignore("No idea why is this now failing on circleci")
    @Test public void
    can_access_host_networked_ports() throws Exception {
        // On linux the docker host is running on localhost, so host ports are accessible through localhost.
        // On Windows and Mac however, docker is being run in a linux VM. As such the docker host is the running
        // VM, not localhost, and the ports are inaccessible from outside the VM.
        // As such, this test will only run on linux.
        assumeTrue("Host ports are only accessible on linux", SystemUtils.IS_OS_LINUX);

        DockerComposeRule docker = DockerComposeRule.builder()
                .file("src/test/resources/host-networked-docker-compose.yaml")
                .waitingForHostNetworkedPort(5432, toBeOpen())
                .build();
        try {
            docker.before();
            assertThat(docker.hostNetworkedPort(5432).getInternalPort(), is(5432));
            assertThat(docker.hostNetworkedPort(5432).getExternalPort(), is(5432));
        } finally {
            docker.after();
        }
    }
}
