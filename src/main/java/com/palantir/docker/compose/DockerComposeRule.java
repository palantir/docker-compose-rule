/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.compose;

import static com.palantir.docker.compose.connection.waiting.ClusterHealthCheck.serviceHealthCheck;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.ImmutableCluster;
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
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
    public static final Duration DEFAULT_TIMEOUT = Duration.standardMinutes(2);
    public static final int DEFAULT_RETRY_ATTEMPTS = 2;

    private static final Logger log = LoggerFactory.getLogger(DockerComposeRule.class);

    public abstract DockerComposeFiles files();

    protected abstract List<ClusterWait> clusterWaits();

    @Value.Default
    public DockerMachine machine() {
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
    public DockerCompose dockerCompose() {
        DockerCompose dockerCompose = new DefaultDockerCompose(executable(), machine());
        return new RetryingDockerCompose(retryAttempts(), dockerCompose);
    }

    @Value.Default
    public Cluster containers() {
        return ImmutableCluster.builder()
                .ip(machine().getIp())
                .containerCache(new ContainerCache(dockerCompose()))
                .build();
    }

    @Value.Default
    protected int retryAttempts() {
        return DEFAULT_RETRY_ATTEMPTS;
    }

    @Value.Default
    protected boolean skipShutdown() {
        return false;
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
        clusterWaits().forEach(clusterWait -> clusterWait.waitUntilReady(containers()));
        log.debug("docker-compose cluster started");
    }

    @Override
    public void after() {
        try {
            if (skipShutdown()) {
                log.error("******************************************************************************************\n"
                        + "* docker-compose-rule has been configured to skip docker-compose shutdown:               *\n"
                        + "* this means the containers will be left running after tests finish executing.           *\n"
                        + "* If you see this message when running on CI it means you are potentially abandoning     *\n"
                        + "* long running processes and leaking resources.                                          *\n"
                        + "*******************************************************************************************");
            } else {
                log.debug("Killing docker-compose cluster");
                dockerCompose().down();
                dockerCompose().kill();
                dockerCompose().rm();
            }

            logCollector().stopCollecting();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error cleaning up docker compose cluster", e);
        }
    }

    public void exec(DockerComposeExecOption options, String containerName,
            DockerComposeExecArgument arguments) throws IOException, InterruptedException {
        dockerCompose().exec(options, containerName, arguments);
    }

    public static ImmutableDockerComposeRule.Builder builder() {
        return ImmutableDockerComposeRule.builder();
    }

    public abstract static class Builder {

        public abstract ImmutableDockerComposeRule.Builder files(DockerComposeFiles files);

        public ImmutableDockerComposeRule.Builder file(String dockerComposeYmlFile) {
            return files(DockerComposeFiles.from(dockerComposeYmlFile));
        }

        public abstract ImmutableDockerComposeRule.Builder logCollector(LogCollector logCollector);

        public ImmutableDockerComposeRule.Builder saveLogsTo(String path) {
            return logCollector(FileLogCollector.fromPath(path));
        }

        public abstract ImmutableDockerComposeRule.Builder addClusterWait(ClusterWait clusterWait);

        public ImmutableDockerComposeRule.Builder waitingForService(String serviceName, HealthCheck<Container> healthCheck) {
            return waitingForService(serviceName, healthCheck, DEFAULT_TIMEOUT);
        }

        public ImmutableDockerComposeRule.Builder waitingForService(String serviceName, HealthCheck<Container> healthCheck, Duration timeout) {
            ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(serviceName, healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        public ImmutableDockerComposeRule.Builder waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck) {
            return waitingForServices(services, healthCheck, DEFAULT_TIMEOUT);
        }

        public ImmutableDockerComposeRule.Builder waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck, Duration timeout) {
            ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(services, healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }
    }
}
