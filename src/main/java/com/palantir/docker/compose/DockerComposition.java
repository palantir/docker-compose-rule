package com.palantir.docker.compose;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.joda.time.Duration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionTimeoutException;

public class DockerComposition extends ExternalResource {

    private static final Logger log = LoggerFactory.getLogger(DockerComposition.class);

    private final DockerComposeExecutable dockerComposeProcess;
    private final DockerMachine dockerMachine;
    private final Map<String, Container> containers;
    private final Map<Container, Function<Container, Boolean>> servicesToWaitFor;
    private final Duration serviceTimeout;
    private final LogCollector logCollector;

    public DockerComposition(String dockerComposeFile) {
        this(new DockerComposeExecutable(new File(dockerComposeFile)), DockerMachine.fromEnvironment());
        log.debug("Using docker-compose file '{}'", dockerComposeFile);
    }

    public DockerComposition(DockerComposeExecutable dockerComposeProcess, DockerMachine dockerMachine) {
        this(dockerComposeProcess, dockerMachine, new HashMap<>(), Duration.standardMinutes(2), new DoNothingLogCollector(), new HashMap<>());
    }

    private DockerComposition(DockerComposeExecutable dockerComposeProcess,
                              DockerMachine dockerMachine,
                              Map<Container, Function<Container, Boolean>> servicesToWaitFor,
                              Duration serviceTimeout,
                              LogCollector logCollector,
                              Map<String, Container> containers) {
                this.dockerComposeProcess = dockerComposeProcess;
                this.dockerMachine = dockerMachine;
                this.servicesToWaitFor = servicesToWaitFor;
                this.serviceTimeout = serviceTimeout;
                this.logCollector = logCollector;
                this.containers = new HashMap<>(containers);
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
            dockerComposeProcess.kill();
            dockerComposeProcess.rm();
            logCollector.stopCollecting();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error cleaning up docker compose cluster", e);
        }
    }

    public DockerPort portOnContainerWithExternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return service(container).portMappedExternallyTo(portNumber);
    }

    public DockerPort portOnContainerWithInternalMapping(String container, int portNumber) throws IOException, InterruptedException {
        return service(container).portMappedInternallyTo(portNumber);
    }

    public DockerComposition waitingForService(String serviceName) {
        return waitingForService(serviceName, (container) -> container.waitForPorts(serviceTimeout));
    }

    public DockerComposition waitingForHttpService(String serviceName, int internalPort, Function<DockerPort, String> urlFunction) {
        return waitingForService(serviceName, (container) -> container.waitForHttpPort(internalPort, urlFunction, serviceTimeout));
    }

    public DockerComposition waitingForService(String serviceName, Function<Container, Boolean> check) {
        Map<Container, Function<Container, Boolean>> services = new HashMap<>(servicesToWaitFor);
        services.put(service(serviceName), check);
        return new DockerComposition(dockerComposeProcess,
                                     dockerMachine,
                                     services,
                                     serviceTimeout,
                                     logCollector,
                                     containers);
    }

    public DockerComposition serviceTimeout(Duration timeout) {
        return new DockerComposition(dockerComposeProcess,
                                     dockerMachine,
                                     servicesToWaitFor,
                                     timeout,
                                     logCollector,
                                     containers);
    }

    private Container service(String serviceName) {
        if (!containers.containsKey(serviceName)) {
            containers.put(serviceName, new Container(serviceName, dockerComposeProcess, dockerMachine));
        }
        return containers.get(serviceName);
    }

    public DockerComposition saveLogsTo(String path) {
        File logDirectory = new File(path);
        Validate.isTrue(!logDirectory.isFile(), "Log directory cannot be a file");
        if (!logDirectory.exists()) {
            Validate.isTrue(logDirectory.mkdirs(), "Error making log directory");
        }
        FileLogCollector newLogCollector = new FileLogCollector(logDirectory);
        return new DockerComposition(dockerComposeProcess,
                                     dockerMachine,
                                     servicesToWaitFor,
                                     serviceTimeout,
                                     newLogCollector,
                                     containers);
    }

}
