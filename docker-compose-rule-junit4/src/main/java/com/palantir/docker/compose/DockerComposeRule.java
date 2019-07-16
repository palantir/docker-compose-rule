/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */
package com.palantir.docker.compose;

import static com.palantir.docker.compose.connection.waiting.ClusterHealthCheck.serviceHealthCheck;
import static com.palantir.docker.compose.connection.waiting.ClusterHealthCheck.transformingHealthCheck;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.palantir.docker.compose.EventEmitter.InterruptableClusterWait;
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
import com.palantir.docker.compose.events.EventConsumer;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;
import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
@CustomImmutablesStyle
public abstract class DockerComposeRule extends ExternalResource {
    private static final Logger log = LoggerFactory.getLogger(DockerComposeRule.class);

    public static final Duration DEFAULT_TIMEOUT = Duration.standardMinutes(2);
    public static final int DEFAULT_RETRY_ATTEMPTS = 2;

    public DockerPort hostNetworkedPort(int port) {
        return new DockerPort(machine().getIp(), port, port);
    }

    public abstract DockerComposeFiles files();

    protected abstract List<ClusterWait> clusterWaits();

    protected abstract List<EventConsumer> eventConsumers();

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

    @Value.Derived
    protected EventEmitter emitEventsFor() {
        return new EventEmitter(eventConsumers());
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    @Override
    public void before() throws IOException, InterruptedException {
        log.debug("Starting docker-compose cluster");

        pullBuildAndUp();

        emitEventsFor().waitingForServices(this::waitForServices);
    }

    private void pullBuildAndUp() throws IOException, InterruptedException {
        if (pullOnStartup()) {
            emitEventsFor().pull(dockerCompose()::pull);
        }

        emitEventsFor().build(dockerCompose()::build);

        DockerCompose upDockerCompose = dockerCompose();
        if (removeConflictingContainersOnStartup()) {
            upDockerCompose = new ConflictingContainerRemovingDockerCompose(upDockerCompose, docker());
        }

        emitEventsFor().up(upDockerCompose::up);
    }

    private void waitForServices() throws InterruptedException {
        log.debug("Waiting for services");
        InterruptableClusterWait nativeHealthCheckClusterWait =
                emitEventsFor().nativeClusterWait(
                        new ClusterWait(ClusterHealthCheck.nativeHealthChecks(), nativeServiceHealthCheckTimeout()));

        List<InterruptableClusterWait> allClusterWaits = Stream.concat(
                Stream.of(nativeHealthCheckClusterWait),
                clusterWaits().stream().map(emitEventsFor()::userClusterWait))
                .collect(Collectors.toList());

        waitForAllClusterWaits(allClusterWaits);

        log.debug("docker-compose cluster started");
    }

    private void waitForAllClusterWaits(List<InterruptableClusterWait> allClusterWaits) throws InterruptedException {
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                allClusterWaits.size(),
                new ThreadFactoryBuilder()
                        .setNameFormat("dcr-wait-%d")
                        .build()));

        try {
            ListenableFuture<?> listListenableFuture =
                    Futures.allAsList(allClusterWaits.stream()
                    .map(clusterWait -> executorService.submit(() -> {
                        try {
                            clusterWait.waitForCluster(containers());
                        } catch (InterruptedException e) {
                            if (executorService.isShutdown()) {
                                // ignore if this InterruptedException has occurred because we shut down and
                                // terminated the executor
                                return;
                            }

                            Throwables.propagate(e);
                        }
                    }))
                    .collect(Collectors.toList()));

            listListenableFuture.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new IllegalStateException("A cluster wait errored out: ", e);
        } finally {
            MoreExecutors.shutdownAndAwaitTermination(executorService, 0, TimeUnit.SECONDS);
        }
    }

    public void after() {
        try {
            emitEventsFor().shutdownStop(() ->
                    shutdownStrategy().stop(this.dockerCompose()));

            emitEventsFor().logCollection(() ->
                    logCollector().collectLogs(this.dockerCompose()));

            emitEventsFor().shutdown(() ->
                    shutdownStrategy().shutdown(this.dockerCompose(), this.docker()));
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
         * Deprecated.
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

        public Builder clusterWaits(Iterable<? extends ClusterWait> elements) {
            return addAllClusterWaits(elements);
        }

        @Override
        public DockerComposeRule build() {
            return super.build();
        }
    }

}
