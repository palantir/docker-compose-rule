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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DockerComposeFilesTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void notSpecifyingAComposeFileResultsInError() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("A docker compose file must be specified.");
        DockerComposeFiles.from();
    }

    @Test
    public void missingDockerComposeFileThrowsAnException() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("The following docker-compose files:");
        exception.expectMessage("does-not-exist.yaml");
        exception.expectMessage("do not exist.");
        DockerComposeFiles.from("does-not-exist.yaml");
    }

    @Test
    public void aSingleMissingComposeFileWithAnExistingComposeFileThrowsCorrectException() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("The following docker-compose files:");
        exception.expectMessage("does-not-exist.yaml");
        exception.expectMessage("do not exist.");
        exception.expectMessage(not(containsString("docker-compose.yaml")));

        File composeFile = tempFolder.newFile("docker-compose.yaml");
        DockerComposeFiles.from("does-not-exist.yaml", composeFile.getAbsolutePath());
    }

    @Test
    public void dockerComposeFileCommandGetsGeneratedCorrectly_singleComposeFile() throws Exception {
        File composeFile = tempFolder.newFile("docker-compose.yaml");
        DockerComposeFiles dockerComposeFiles = DockerComposeFiles.from(composeFile.getAbsolutePath());
        assertThat(dockerComposeFiles.constructComposeFileCommand(), contains("--file", composeFile.getAbsolutePath()));
    }

    @Test
    public void dockerComposeFileCommandGetsGeneratedCorrectly_multipleComposeFile() throws Exception {
        File composeFile1 = tempFolder.newFile("docker-compose1.yaml");
        File composeFile2 = tempFolder.newFile("docker-compose2.yaml");
        DockerComposeFiles dockerComposeFiles = DockerComposeFiles.from(composeFile1.getAbsolutePath(), composeFile2.getAbsolutePath());
        assertThat(dockerComposeFiles.constructComposeFileCommand(), contains(
                "--file", composeFile1.getAbsolutePath(), "--file", composeFile2.getAbsolutePath()));
    }

}
