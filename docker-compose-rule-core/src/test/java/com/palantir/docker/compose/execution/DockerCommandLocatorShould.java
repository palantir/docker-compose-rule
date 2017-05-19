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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class DockerCommandLocatorShould {
    private static final String command = "command";
    private static final String windowsCommand = command + ".exe";

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Rule public ExpectedException exception = ExpectedException.none();

    private final DockerCommandLocator locator = spy(new DockerCommandLocator(command));

    private final Map<String, String> env = new HashMap<>();

    private Path emptyFolder;

    private Path firstFolder;

    private Path secondFolder;

    private String commandFile;

    private String windowsCommandFile;

    private String pathString;

    @Before
    public void setup() throws IOException {
        emptyFolder = folder.newFolder("empty").toPath();
        firstFolder = folder.newFolder("first").toPath();
        secondFolder = folder.newFolder("second").toPath();

        commandFile = Files.createFile(firstFolder.resolve(command)).toString();
        windowsCommandFile = Files.createFile(firstFolder.resolve(windowsCommand)).toString();
        Files.createFile(secondFolder.resolve(command));
        Files.createFile(secondFolder.resolve(windowsCommand));

        pathString = Stream.of(emptyFolder, firstFolder, secondFolder)
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));

        doReturn(env).when(locator).getEnv();
    }

    @Test public void
    provide_the_first_command_location() {
        env.put("path", pathString);
        doReturn(false).when(locator).isWindows();
        assertThat(locator.getLocation(), is(commandFile));
    }

    @Test public void
    provide_the_first_command_location_using_capitalised_path() {
        env.put("PATH", pathString);
        doReturn(false).when(locator).isWindows();
        assertThat(locator.getLocation(), is(commandFile));
    }

    @Test public void
    provide_the_first_command_location_on_windows() {
        env.put("path", pathString);
        doReturn(true).when(locator).isWindows();
        assertThat(locator.getLocation(), is(windowsCommandFile));
    }

    @Test public void
    fail_when_no_paths_contain_command() {
        env.put("path", emptyFolder.toString());
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Could not find " + command + " in path");
        locator.getLocation();
    }

    @Test public void
    fail_when_no_path_variable_is_set() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Could not find path variable in env");
        locator.getLocation();
    }
}
