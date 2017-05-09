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
package com.palantir.docker.compose.execution;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DockerCommandLocatorShould {
    private static final String command = "not-a-real-command!";
    private static final String windowsCommand = command + ".exe";

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Rule public ExpectedException exception = ExpectedException.none();

    @Test public void
    returns_the_command_name_when_no_other_paths_contain_command() {
        DockerCommandLocator locator = DockerCommandLocator.forCommand(command)
                .isWindows(false)
                .build();

        assertThat(locator.getLocation(), is(command));
    }

    @Test public void
    returns_the_command_name_with_exe_on_windows_when_no_other_paths_contain_command() {
        DockerCommandLocator locator = DockerCommandLocator.forCommand(command)
                .isWindows(true)
                .build();

        assertThat(locator.getLocation(), is(windowsCommand));
    }

    @Test public void
    allow_the_path_to_be_overriden() throws IOException {
        Path overrideFolder = folder.newFolder("override").toPath();
        String overridenCommand = Files.createFile(overrideFolder.resolve(command)).toString();

        DockerCommandLocator locator = DockerCommandLocator.forCommand(command)
                .locationOverride(overridenCommand)
                .isWindows(false)
                .build();

        assertThat(locator.getLocation(), is(overridenCommand));
    }

    @Test public void
    search_in_known_mac_install_locations_for_the_command() throws IOException {
        Path macSearchFolder = folder.newFolder("override").toPath();
        String commandLocation = Files.createFile(macSearchFolder.resolve(command)).toString();

        DockerCommandLocator locator = DockerCommandLocator.forCommand(command)
                .macSearchLocations(singletonList(macSearchFolder.toString()))
                .isWindows(false)
                .build();

        assertThat(locator.getLocation(), is(commandLocation));
    }

}
