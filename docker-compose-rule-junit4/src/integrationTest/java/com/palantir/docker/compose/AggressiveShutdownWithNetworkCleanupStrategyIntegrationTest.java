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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.Sets;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import java.util.Arrays;
import java.util.Set;
import org.junit.Test;

public class AggressiveShutdownWithNetworkCleanupStrategyIntegrationTest {

    private final DockerComposeRule rule = DockerComposeRule.builder()
            .file("src/test/resources/shutdown-strategy-with-network.yaml")
            .logCollector(new DoNothingLogCollector())
            .retryAttempts(0)
            .shutdownStrategy(ShutdownStrategy.AGGRESSIVE_WITH_NETWORK_CLEANUP)
            .build();

    @Test
    public void shut_down_multiple_containers_immediately() throws Exception {
        assertThat(rule.dockerCompose().ps(), is(TestContainerNames.of()));

        rule.before();
        assertThat(rule.dockerCompose().ps().size(), is(2));
        rule.after();

        assertThat(rule.dockerCompose().ps(), is(TestContainerNames.of()));
    }

    @Test
    public void clean_up_created_networks_when_shutting_down() throws Exception {
        Set<String> networksBeforeRun = parseLinesFromOutputString(rule.docker().listNetworks());

        rule.before();
        assertThat(parseLinesFromOutputString(rule.docker().listNetworks()), is(not(networksBeforeRun)));
        rule.after();

        assertThat(parseLinesFromOutputString(rule.docker().listNetworks()), is(networksBeforeRun));
    }

    private static Set<String> parseLinesFromOutputString(String output) {
        return Sets.newHashSet(Arrays.asList(output.split("\n")));
    }
}
