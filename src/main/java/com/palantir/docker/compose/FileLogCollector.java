package com.palantir.docker.compose;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLogCollector implements LogCollector {

    private static final Logger log = LoggerFactory.getLogger(FileLogCollector.class);

    private static final long STOP_TIMEOUT_IN_MILLIS = 50;

    private final File logDirectory;

    private ExecutorService executor = null;

    public FileLogCollector(File logDirectory) {
        this.logDirectory = logDirectory;
    }

    @Override
    public synchronized void startCollecting(DockerComposeExecutable dockerCompose) throws IOException, InterruptedException {
        if (executor != null) {
            throw new RuntimeException("Cannot start collecting the same logs twice");
        }
        ContainerNames containerNames = dockerCompose.ps();
        if (containerNames.size() == 0) {
            return;
        }
        executor = Executors.newFixedThreadPool(containerNames.size());
        containerNames.stream()
                      .forEachOrdered(container -> this.collectLogs(container, dockerCompose));
    }

    private void collectLogs(String container, DockerComposeExecutable dockerCompose)  {
        executor.submit(() -> {
            File outputFile = new File(logDirectory, container + ".log");
            log.info("Writing logs for container '{}' to '{}'", container, outputFile.getAbsolutePath());
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                dockerCompose.writeLogs(container, outputStream);
            } catch (IOException e) {
                throw new RuntimeException("Error reading log", e);
            }
        });
    }

    @Override
    public synchronized void stopCollecting() throws InterruptedException {
        if (executor == null) {
            return;
        }
        if (!executor.awaitTermination(STOP_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS)) {
            log.warn("docker containers were still running when log collection stopped");
            executor.shutdownNow();
        }
    }

}
