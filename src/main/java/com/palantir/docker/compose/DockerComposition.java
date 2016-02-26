package com.palantir.docker.compose;

import com.google.common.collect.ImmutableMap;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import com.palantir.docker.compose.logging.FileLogCollector;
import com.palantir.docker.compose.logging.LogCollector;
import org.apache.commons.lang3.Validate;
import org.joda.time.Duration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class DockerComposition extends ExternalResource {

    private static final Logger log = LoggerFactory.getLogger(DockerComposition.class);

    private final DockerComposeExecutable dockerComposeProcess;
    private final ContainerCache containers;
    private final Map<Container, Function<Container, Boolean>> servicesToWaitFor;
    private final Duration serviceTimeout;
    private final LogCollector logCollector;

    public static DockerCompositionBuilder of(String dockerComposeFile, DockerMachine dockerMachine) {
        return of(new DockerComposeExecutable(new File(dockerComposeFile), dockerMachine));
    }

    public static DockerCompositionBuilder of(DockerComposeExecutable executable) {
        return new DockerCompositionBuilder(executable);
    }

    private DockerComposition(DockerComposeExecutable dockerComposeProcess,
                              Map<Container, Function<Container, Boolean>> servicesToWaitFor,
                              Duration serviceTimeout,
                              LogCollector logCollector,
                              ContainerCache containers) {
        this.dockerComposeProcess = dockerComposeProcess;
        this.servicesToWaitFor = ImmutableMap.copyOf(servicesToWaitFor);
        this.serviceTimeout = serviceTimeout;
        this.logCollector = logCollector;
        this.containers = containers;
    }

    @Override
    public void before() throws IOException, InterruptedException {
        log.debug("Starting docker-compose cluster");
        dockerComposeProcess.build();
        dockerComposeProcess.up();

        log.debug("Starting log collection");

        logCollector.startCollecting(dockerComposeProcess);
        log.debug("Waiting for services");
        servicesToWaitFor.entrySet().forEach(serviceCheck -> waitForService(serviceCheck.getKey(), serviceCheck.getValue()));
        log.debug("docker-compose cluster started");
    }

    private void waitForService(Container service, Function<Container, Boolean> serviceCheck) {
        log.debug("Waiting for service '{}'", service);
        try {
            Awaitility.await()
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .atMost(serviceTimeout.getMillis(), TimeUnit.MILLISECONDS)
                .until(() -> serviceCheck.apply(service));
        } catch (ConditionTimeoutException e) {
            throw new IllegalStateException("Container '" + service.getContainerName() + "' failed to pass startup check");
        }
    }

    @Override
    public void after() {
        try {
            log.debug("Killing docker-compose cluster");
            dockerComposeProcess.down();
            dockerComposeProcess.kill();
            dockerComposeProcess.rm();
            logCollector.stopCollecting();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error cleaning up docker compose cluster", e);
        }
    }

    public DockerPort portOnContainerWithExternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return containers.get(container)
                         .portMappedExternallyTo(portNumber);
    }

    public DockerPort portOnContainerWithInternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return containers.get(container)
                         .portMappedInternallyTo(portNumber);
    }

    public static class DockerCompositionBuilder {

        private final Map<String, Function<Container, Boolean>> containersToWaitFor = new HashMap<>();
        private final DockerComposeExecutable dockerComposeProcess;
        private final ContainerCache containers;
        private Duration serviceTimeout = Duration.standardMinutes(2);
        private LogCollector logCollector = new DoNothingLogCollector();

        public DockerCompositionBuilder(DockerComposeExecutable dockerComposeProcess) {
            this.dockerComposeProcess = dockerComposeProcess;
            this.containers = new ContainerCache(dockerComposeProcess);
        }

        public DockerCompositionBuilder waitingForService(String serviceName) {
            return waitingForService(serviceName, (container) -> container.waitForPorts(serviceTimeout));
        }

        public DockerCompositionBuilder waitingForHttpService(String serviceName, int internalPort, Function<DockerPort, String> urlFunction) {
            return waitingForService(serviceName, (container) -> container.waitForHttpPort(internalPort, urlFunction, serviceTimeout));
        }

        public DockerCompositionBuilder waitingForService(String serviceName, Function<Container, Boolean> check) {
            containersToWaitFor.put(serviceName, check);
            return this;
        }

        public DockerCompositionBuilder serviceTimeout(Duration timeout) {
            this.serviceTimeout = timeout;
            return this;
        }

        public DockerCompositionBuilder saveLogsTo(String path) {
            File logDirectory = new File(path);
            Validate.isTrue(!logDirectory.isFile(), "Log directory cannot be a file");
            if (!logDirectory.exists()) {
                Validate.isTrue(logDirectory.mkdirs(), "Error making log directory");
            }
            this.logCollector = new FileLogCollector(logDirectory);
            return this;
        }

        public DockerComposition build() {
            Map<Container, Function<Container, Boolean>> servicesToWaitFor = containersToWaitFor.entrySet()
                                                                                                .stream()
                                                                                                .collect(toMap(e -> containers.get(e.getKey()), Map.Entry::getValue));
            return new DockerComposition(dockerComposeProcess, servicesToWaitFor, serviceTimeout, logCollector, containers);
        }

    }

}
