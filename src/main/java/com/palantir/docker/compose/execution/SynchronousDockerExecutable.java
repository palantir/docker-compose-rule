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

import static com.google.common.base.Throwables.propagate;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class SynchronousDockerExecutable {
    public static final int HOURS_TO_WAIT_FOR_STD_OUT_TO_CLOSE = 12;
    public static final int MINUTES_TO_WAIT_AFTER_STD_OUT_CLOSES = 1;
    private final DockerExecutable dockerExecutable;
    private final Consumer<String> logConsumer;

    public SynchronousDockerExecutable(DockerExecutable dockerComposeExecutable, Consumer<String> logConsumer) {
        this.dockerExecutable = dockerComposeExecutable;
        this.logConsumer = logConsumer;
    }

    public ProcessResult run(String... commands) throws IOException, InterruptedException {
        Process process = dockerExecutable.execute(commands);

        Future<String> outputProcessing = newSingleThreadExecutor()
                .submit(() -> processOutputFrom(process));

        String output = waitForResultFrom(outputProcessing);

        process.waitFor(MINUTES_TO_WAIT_AFTER_STD_OUT_CLOSES, TimeUnit.MINUTES);

        return new ProcessResult(process.exitValue(), output);
    }

    private String processOutputFrom(Process process) {
        return asReader(process.getInputStream()).lines()
                .peek(logConsumer)
                .collect(joining(System.lineSeparator()));
    }

    private String waitForResultFrom(Future<String> outputProcessing) {
        try {
            return outputProcessing.get(HOURS_TO_WAIT_FOR_STD_OUT_TO_CLOSE, TimeUnit.HOURS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw propagate(e);
        }
    }

    private BufferedReader asReader(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, UTF_8));
    }
}
