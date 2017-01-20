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
package com.palantir.docker.compose.configuration;

import static com.palantir.docker.compose.configuration.EnvironmentVariables.TCP_PROTOCOL;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RemoteHostIpResolverShould {

    private static final String IP = "192.168.99.100";
    private static final int PORT = 2376;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void result_in_error_blank_when_resolving_invalid_docker_host() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("DOCKER_HOST cannot be blank/null");
        new RemoteHostIpResolver().resolveIp("");
    }

    @Test
    public void result_in_error_null_when_resolving_invalid_docker_host() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("DOCKER_HOST cannot be blank/null");
        new RemoteHostIpResolver().resolveIp(null);
    }

    @Test
    public void resolve_docker_host_with_port() throws Exception {
        String dockerHost = String.format("%s%s:%d", TCP_PROTOCOL, IP, PORT);
        assertThat(new RemoteHostIpResolver().resolveIp(dockerHost), Matchers.is(IP));
    }

    @Test
    public void resolve_docker_host_without_port() throws Exception {
        String dockerHost = String.format("%s%s", TCP_PROTOCOL, IP);
        assertThat(new RemoteHostIpResolver().resolveIp(dockerHost), Matchers.is(IP));
    }
}
