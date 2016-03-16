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
package com.palantir.docker.compose.logging;

import com.palantir.docker.compose.DockerComposition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static com.palantir.docker.compose.connection.waiting.HealthChecks.toHaveAllPortsOpen;
import static com.palantir.docker.compose.matchers.IOMatchers.file;
import static com.palantir.docker.compose.matchers.IOMatchers.fileWithName;
import static com.palantir.docker.compose.matchers.IOMatchers.matchingPattern;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.core.Is.is;

public class DockerCompositionLoggingIntegrationTest {

    @Rule
    public TemporaryFolder logFolder = new TemporaryFolder();

    private DockerComposition loggingComposition;

    @Before
    public void setUp() throws Exception {
        loggingComposition = DockerComposition.of("src/test/resources/docker-compose.yaml")
                .waitingForService("db", toHaveAllPortsOpen())
                .waitingForService("db2", toHaveAllPortsOpen())
                .saveLogsTo(logFolder.getRoot().getAbsolutePath())
                .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void logs_can_be_saved_to_a_directory() throws IOException, InterruptedException {
        try {
            loggingComposition.before();
        } finally {
            loggingComposition.after();
        }
        assertThat(logFolder.getRoot().listFiles(), arrayContainingInAnyOrder(fileWithName("db.log"), fileWithName("db2.log")));
        assertThat(new File(logFolder.getRoot(), "db.log"), is(file(matchingPattern(".*Attaching to \\w+_db_1.*"))));
        assertThat(new File(logFolder.getRoot(), "db2.log"), is(file(matchingPattern(".*Attaching to \\w+_db2_1.*"))));
    }

}
