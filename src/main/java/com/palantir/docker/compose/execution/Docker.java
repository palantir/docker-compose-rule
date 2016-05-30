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

import static java.util.stream.Collectors.joining;

import com.google.common.collect.ObjectArrays;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Docker {
    private static final Logger log = LoggerFactory.getLogger(Docker.class);

    private final SynchronousDockerExecutable executable;

    public Docker(DockerExecutable rawExecutable) {
        this.executable = new SynchronousDockerExecutable(rawExecutable, log::debug);
    }

    public void rm(String... containerNames) throws IOException, InterruptedException {
        executeCommand(throwingOnError(), ObjectArrays.concat(new String[] {"rm", "-f"}, containerNames, String.class));
    }

    protected final String executeCommand(ErrorHandler errorHandler, String... commands)
            throws IOException, InterruptedException {
        ProcessResult result = executable.run(commands);

        if (result.exitCode() != 0) {
            errorHandler.handle(result.exitCode(), result.output(), commands);
        }

        return result.output();
    }

    protected final ErrorHandler throwingOnError() {
        return (exitCode, output, commands) -> {
            String message = constructNonZeroExitErrorMessage(exitCode, commands) + "\nThe output was:\n" + output;
            throw new DockerComposeExecutionException(message);
        };
    }

    private String constructNonZeroExitErrorMessage(int exitCode, String... commands) {
        return "'docker " + Arrays.stream(commands).collect(joining(" ")) + "' returned exit code " + exitCode;
    }

}
