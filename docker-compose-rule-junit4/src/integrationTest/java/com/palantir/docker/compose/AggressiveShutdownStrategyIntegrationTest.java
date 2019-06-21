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

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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

        MatcherAssert.assertThat(rule.dockerCompose().ps(), Matchers.is(TestContainerNames.of()));

        rule.before();
        assertThat(rule.dockerCompose().ps().size(), is(2));
        rule.after();

        assertThat(rule.dockerCompose().ps(), is(TestContainerNames.of()));
    }

}
