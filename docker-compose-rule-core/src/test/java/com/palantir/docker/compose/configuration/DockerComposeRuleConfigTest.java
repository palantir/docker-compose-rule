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

package com.palantir.docker.compose.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.reporting.ReportingConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DockerComposeRuleConfigTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void can_derserialize_config_found_one_dir_up() throws IOException {
        File config = temporaryFolder.newFile(".docker-compose-rule.yml");
        Files.write(config.toPath(), ImmutableList.of(
                "reporting:",
                "  url: http://example.com/",
                "  environmentVariableWhitelist: ['^foobar$']"
        ), StandardCharsets.UTF_8);

        File startDir = temporaryFolder.newFolder("start-dir");

        assertThat(DockerComposeRuleConfig.findAutomaticallyFrom(startDir)).hasValue(DockerComposeRuleConfig.builder()
                .reporting(ReportingConfig.builder()
                        .url("http://example.com/")
                        .addEnvironmentVariableWhitelist("^foobar$")
                        .build())
                .build());
    }

    @Test
    public void optional_empty_when_config_does_not_exist() {
        assertThat(DockerComposeRuleConfig.findAutomaticallyFrom(temporaryFolder.getRoot())).isEmpty();
    }

    @Test
    public void throws_when_config_file_is_invalid() throws IOException {
        File config = temporaryFolder.newFile(".docker-compose-rule.yml");
        Files.write(config.toPath(), ImmutableList.of(
                "reporting:",
                "  whoops: oh no"
        ), StandardCharsets.UTF_8);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> DockerComposeRuleConfig.findAutomaticallyFrom(temporaryFolder.getRoot()))
                .withMessageContaining("deserialize config file");
    }

}
