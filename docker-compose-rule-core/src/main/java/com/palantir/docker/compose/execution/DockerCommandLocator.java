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

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DockerCommandLocator {

    protected abstract String command();

    @Value.Default
    protected boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    @Value.Derived
    protected String executableName() {
        if (isWindows()) {
            return command() + ".exe";
        }
        return command();
    }

    @Nullable
    protected abstract String locationOverride();

    @Value.Derived
    protected DockerCommandLocations searchLocations() {
        return DockerCommandLocations.withLocationOverride(locationOverride());
    }

    public String getLocation() {
        return searchLocations()
                .forCommand()
                .map(p -> p.resolve(executableName()))
                .filter(Files::exists)
                .findFirst()
                .map(Path::toString)
                .orElseThrow(() -> new IllegalStateException("Could not find " + command() + " in path"));
    }

    public static ImmutableDockerCommandLocator.Builder forCommand(String command) {
        return ImmutableDockerCommandLocator.builder()
                .command(command);
    }

}
