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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;

public class DockerCommandLocator {
    private static final Pattern PATH_SPLITTER = Pattern.compile(File.pathSeparator);

    private final String command;

    public DockerCommandLocator(String command) {
        this.command = command;
    }

    public String getLocation() {
        // Get path variable, ignoring the case of its name
        String path = getEnv().entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase("path"))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new IllegalStateException("Could not find path variable in env"));

        // The filename is the same as the command, except on Windows where it ends with ".exe"
        String filename = isWindows() ? command + ".exe" : command;

        // Look through the directories in path for the given command which must exist and be executable
        return PATH_SPLITTER.splitAsStream(path)
                .map(p -> Paths.get(p, filename))
                .filter(Files::exists)
                .findFirst()
                .map(Path::toString)
                .orElseThrow(() -> new IllegalStateException("Could not find " + command + " in path"));
    }

    protected Map<String, String> getEnv() {
        return System.getenv();
    }

    protected boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    @Override
    public String toString() {
        return "DockerCommandLocator{command=" + command + "}";
    }
}
