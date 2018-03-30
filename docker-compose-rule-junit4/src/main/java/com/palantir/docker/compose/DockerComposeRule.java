/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.compose;

import static com.palantir.docker.compose.connection.waiting.ClusterHealthCheck.serviceHealthCheck;
import static com.palantir.docker.compose.connection.waiting.ClusterHealthCheck.transformingHealthCheck;

import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.ImmutableCluster;
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.connection.waiting.HealthCheck;
import com.palantir.docker.compose.execution.ConflictingContainerRemovingDockerCompose;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecArgument;
import com.palantir.docker.compose.execution.DockerComposeExecOption;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.execution.DockerComposeRunArgument;
import com.palantir.docker.compose.execution.DockerComposeRunOption;
import com.palantir.docker.compose.execution.DockerExecutable;
import com.palantir.docker.compose.execution.RetryingDockerCompose;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import com.palantir.docker.compose.logging.FileLogCollector;
import com.palantir.docker.compose.logging.LogCollector;
import com.palantir.docker.compose.logging.LogDirectory;
import java.io.IOException;
import java.util.List;
import org.immutables.value.Value;
import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
@CustomImmutablesStyle
public abstract class DockerComposeRule extends ExternalResource {
    public static final Duration DEFAULT_TIMEOUT = Duration.standardMinutes(2);
    public static final int DEFAULT_RETRY_ATTEMPTS = 2;

    private static final Logger log = LoggerFactory.getLogger(DockerComposeRule.class);

    public DockerPort hostNetworkedPort(int port) {
        return new DockerPort(machine().getIp(), port, port);
    }

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
    public DockerComposeExecutable dockerComposeExecutable() {
        return DockerComposeExecutable.builder()
            .dockerComposeFiles(files())
            .dockerConfiguration(machine())
            .projectName(projectName())
            .build();
    }

    @Value.Default
    public DockerExecutable dockerExecutable() {
        return DockerExecutable.builder()
                .dockerConfiguration(machine())
                .build();
    }

    @Value.Default
    public Docker docker() {
        return new Docker(dockerExecutable());
    }

    @Value.Default
    public ShutdownStrategy shutdownStrategy() {
        return ShutdownStrategy.KILL_DOWN;
    }

    @Value.Default
    public DockerCompose dockerCompose() {
        DockerCompose dockerCompose = new DefaultDockerCompose(dockerComposeExecutable(), machine());
        return new RetryingDockerCompose(retryAttempts(), dockerCompose);
    }

    @Value.Default
    public Cluster containers() {
        return ImmutableCluster.builder()
                .ip(machine().getIp())
                .containerCache(new ContainerCache(docker(), dockerCompose()))
                .build();
    }

    @Value.Default
    protected int retryAttempts() {
        return DEFAULT_RETRY_ATTEMPTS;
    }

    @Value.Default
    protected boolean removeConflictingContainersOnStartup() {
        return true;
    }

    @Value.Default
    protected boolean pullOnStartup() {
        return false;
    }

    @Value.Default
    protected ReadableDuration nativeServiceHealthCheckTimeout() {
        return DEFAULT_TIMEOUT;
    }

    @Value.Default
    protected LogCollector logCollector() {
        return new DoNothingLogCollector();
    }

    @Override
    public void before() throws IOException, InterruptedException {
        log.debug("Starting docker-compose cluster");
        if (pullOnStartup()) {
            dockerCompose().pull();
        }

        dockerCompose().build();

        DockerCompose upDockerCompose = dockerCompose();
        if (removeConflictingContainersOnStartup()) {
            upDockerCompose = new ConflictingContainerRemovingDockerCompose(upDockerCompose, docker());
        }
        upDockerCompose.up();

        logCollector().startCollecting(dockerCompose());
        log.debug("Waiting for services");
        new ClusterWait(ClusterHealthCheck.nativeHealthChecks(), nativeServiceHealthCheckTimeout())
                .waitUntilReady(containers());
        clusterWaits().forEach(clusterWait -> clusterWait.waitUntilReady(containers()));
        log.debug("docker-compose cluster started");
    }

    @Override
    public void after() {
        try {
            shutdownStrategy().shutdown(this.dockerCompose(), this.docker());
            logCollector().stopCollecting();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error cleaning up docker compose cluster", e);
        }
    }

    public String exec(DockerComposeExecOption options, String containerName,
            DockerComposeExecArgument arguments) throws IOException, InterruptedException {
        return dockerCompose().exec(options, containerName, arguments);
    }

    public String run(DockerComposeRunOption options, String containerName,
            DockerComposeRunArgument arguments) throws IOException, InterruptedException {
        return dockerCompose().run(options, containerName, arguments);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ImmutableDockerComposeRule.Builder {

        public Builder file(String dockerComposeYmlFile) {
            return files(DockerComposeFiles.from(dockerComposeYmlFile));
        }

        /**
         * Save the output of docker logs to files, stored in the <code>path</code> directory.
         *
         * See {@link LogDirectory} for some useful utilities, for example:
         * {@link LogDirectory#circleAwareLogDirectory}.
         *
         * @param path directory into which log files should be saved
         */
        public Builder saveLogsTo(String path) {
            return logCollector(FileLogCollector.fromPath(path));
        }

        /**
         * @deprecated Please use {@link DockerComposeRule#shutdownStrategy()} with {@link ShutdownStrategy#SKIP} instead.
         */
        @Deprecated
        public Builder skipShutdown(boolean skipShutdown) {
            if (skipShutdown) {
                return shutdownStrategy(ShutdownStrategy.SKIP);
            }

            return this;
        }

        public Builder waitingForService(String serviceName, HealthCheck<Container> healthCheck) {
            return waitingForService(serviceName, healthCheck, DEFAULT_TIMEOUT);
        }

        public Builder waitingForService(String serviceName, HealthCheck<Container> healthCheck, ReadableDuration timeout) {
            ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(serviceName, healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        public Builder waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck) {
            return waitingForServices(services, healthCheck, DEFAULT_TIMEOUT);
        }

        public Builder waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck, ReadableDuration timeout) {
            ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(services, healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        public Builder waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck) {
            return waitingForHostNetworkedPort(port, healthCheck, DEFAULT_TIMEOUT);
        }

        public Builder waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck, ReadableDuration timeout) {
            ClusterHealthCheck clusterHealthCheck = transformingHealthCheck(cluster -> new DockerPort(cluster.ip(), port, port), healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        public Builder waitingForTextAppearsInLogs(String serviceName, String text) {
            return waitingForTextAppearsInLogs(serviceName, text, DEFAULT_TIMEOUT);
        }

        public Builder waitingForTextAppearsInLogs(String serviceName, String text, ReadableDuration timeout) {
            ClusterHealthCheck logMatcher = cluster -> {
                Container container = cluster.container(serviceName);
                return container.doesTextAppearInLogs(text);
            };
            addClusterWait(new ClusterWait(logMatcher, timeout));
            return this;
        }

        public Builder clusterWaits(Iterable<? extends ClusterWait> elements) {
            return addAllClusterWaits(elements);
        }
    }

}
