/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.compose;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.waiting.MultiServiceHealthCheck;
import com.palantir.docker.compose.connection.waiting.ServiceWait;
import com.palantir.docker.compose.connection.waiting.SingleServiceHealthCheck;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.execution.RetryingDockerCompose;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import com.palantir.docker.compose.logging.FileLogCollector;
import com.palantir.docker.compose.logging.LogCollector;
import java.io.IOException;
import java.util.List;
import org.immutables.value.Value;
import org.joda.time.Duration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
@Value.Style(depluralize = true)
public abstract class DockerComposeRule extends ExternalResource {
    private static final Logger log = LoggerFactory.getLogger(DockerComposeRule.class);

    public abstract DockerComposeFiles files();

    @Value.Default
    protected DockerMachine machine() {
        return DockerMachine.localMachine().build();
    }

    @Value.Default
    public ProjectName projectName() {
        return ProjectName.random();
    }

    @Value.Default
    public DockerComposeExecutable executable() {
        return DockerComposeExecutable.builder()
            .dockerComposeFiles(files())
            .dockerConfiguration(machine())
            .projectName(projectName())
            .build();
    }

    @Value.Default
    protected DockerCompose dockerCompose() {
        DockerCompose dockerCompose = new DefaultDockerCompose(executable(), machine());
        return new RetryingDockerCompose(DockerCompositionBuilder.DEFAULT_RETRY_ATTEMPTS, dockerCompose);
    }

    @Value.Default
    public ContainerCache containers() {
        return new ContainerCache(dockerCompose());
    }

    protected abstract List<DockerService> services();

    @Value.Lazy
    protected List<ServiceWait> serviceWaits() {
        return services().stream()
                .map(service -> {
                    Container container = containers().get(service.serviceName());
                    SingleServiceHealthCheck singlecheck = service.healthCheck();
                    return new ServiceWait(container, singlecheck, timeout());
                }).collect(toList());
    }

    @Value.Default
    protected Duration timeout() {
        return Duration.standardMinutes(5);
    }

    @Value.Default
    protected LogCollector logCollector() {
        return new DoNothingLogCollector();
    }

    @Override
    public void before() throws IOException, InterruptedException {
        log.debug("Starting docker-compose cluster");
        dockerCompose().build();
        dockerCompose().up();

        log.debug("Starting log collection");

        logCollector().startCollecting(dockerCompose());
        log.debug("Waiting for services");
        serviceWaits().forEach(ServiceWait::waitTillServiceIsUp);
        log.debug("docker-compose cluster started");
    }

    @Override
    public void after() {
        try {
            log.debug("Killing docker-compose cluster");
            dockerCompose().down();
            dockerCompose().kill();
            dockerCompose().rm();
            logCollector().stopCollecting();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error cleaning up docker compose cluster", e);
        }
    }

    public static ImmutableDockerComposeRule.Builder builder() {
        return ImmutableDockerComposeRule.builder();
    }

    public abstract static class Builder {

        public abstract Builder logCollector(LogCollector logCollector);

        public Builder saveLogsTo(String path) {
            return logCollector(FileLogCollector.fromPath(path));
        }

        public abstract Builder addService(DockerService element);

        public Builder waitingForService(String serviceName, SingleServiceHealthCheck healthCheck) {
            return addService(DockerService.of(serviceName, healthCheck));
        }

        public Builder waitingForServices(ImmutableList<String> services, MultiServiceHealthCheck healthCheck) {
            return this;
        }
    }
}
