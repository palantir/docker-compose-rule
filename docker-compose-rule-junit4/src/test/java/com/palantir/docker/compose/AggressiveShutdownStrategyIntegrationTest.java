/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import org.junit.Test;

public class AggressiveShutdownStrategyIntegrationTest {

    @Test
    public void shut_down_multiple_containers_immediately() throws Exception {
        DockerComposeRule rule = DockerComposeRule.builder()
                .file("src/test/resources/shutdown-strategy.yaml")
                .logCollector(new DoNothingLogCollector())
                .retryAttempts(0)
                .shutdownStrategy(ShutdownStrategy.AGGRESSIVE)
                .build();

        assertThat(rule.dockerCompose().ps(), is(TestContainerNames.of()));

        rule.before();
        assertThat(rule.dockerCompose().ps().size(), is(2));
        rule.after();

        assertThat(rule.dockerCompose().ps(), is(TestContainerNames.of()));
    }

}
