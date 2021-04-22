/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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

import static com.google.common.base.Preconditions.checkArgument;

import com.palantir.docker.compose.execution.DockerCompose;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileLogCollector implements LogCollector {

    private static final Logger log = LoggerFactory.getLogger(FileLogCollector.class);

    private final File logDirectory;

    public FileLogCollector(File logDirectory) {
        checkArgument(!logDirectory.isFile(), "Log directory cannot be a file");
        if (!logDirectory.exists()) {
            Validate.isTrue(logDirectory.mkdirs(), "Error making log directory: " + logDirectory.getAbsolutePath());
        }
        this.logDirectory = logDirectory;
    }

    public static LogCollector fromPath(String path) {
        return new FileLogCollector(new File(path));
    }

    @Override
    public void collectLogs(DockerCompose dockerCompose) throws IOException, InterruptedException {
        for (String service : dockerCompose.services()) {
            try {
                collectLogs(service, dockerCompose);
            } catch (RuntimeException e) {
                log.error("Failed to collect logs for '{}'", service);
            }
        }
    }

    private void collectLogs(String container, DockerCompose dockerCompose) {
        File outputFile = new File(logDirectory, container + ".log");
        try {
            Files.createFile(outputFile.toPath());
        } catch (final FileAlreadyExistsException e) {
            // ignore
        } catch (final IOException e) {
            throw new RuntimeException("Error creating log file", e);
        }
        log.info("Writing logs for container '{}' to '{}'", container, outputFile.getAbsolutePath());
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            if (!dockerCompose.writeLogs(container, outputStream)) {
                log.error("Timed out while collecting logs for '{}'", container);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading log", e);
        }
    }
}
