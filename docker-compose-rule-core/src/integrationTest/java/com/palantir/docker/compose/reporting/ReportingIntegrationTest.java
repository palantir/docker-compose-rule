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

package com.palantir.docker.compose.reporting;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.DockerComposeManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReportingIntegrationTest {
    @Rule
    public final WireMockRule wireMockRule = new WireMockRule();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void a_report_of_the_right_format_is_posted() throws IOException, InterruptedException {
        File config = temporaryFolder.newFile(".docker-compose-rule.yml");
        Files.write(config.toPath(), ImmutableList.of(
                "reporting:",
                "  url: http://localhost:" + wireMockRule.port() + "/some/path"
        ), StandardCharsets.UTF_8);

        DockerComposeManager dockerComposeManager = changeToDirWithConfigFor(() -> new DockerComposeManager.Builder()
                .file("src/test/resources/no-healthcheck.yaml")
                .build());

        try {
            dockerComposeManager.before();

            wireMockRule.stubFor(post("/some/path").willReturn(status(200)));
        } finally {
            dockerComposeManager.after();
        }

        PostReportOnShutdown.triggerShutdown();

        wireMockRule.verify(postRequestedFor(urlPathEqualTo("/some/path")));
    }

    private <T> T changeToDirWithConfigFor(Supplier<T> supplier) {
        String originalDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", temporaryFolder.getRoot().getAbsolutePath());
            return supplier.get();
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }
}
