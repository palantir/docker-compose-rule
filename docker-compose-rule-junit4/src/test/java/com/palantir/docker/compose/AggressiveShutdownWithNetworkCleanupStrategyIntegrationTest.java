/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
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
