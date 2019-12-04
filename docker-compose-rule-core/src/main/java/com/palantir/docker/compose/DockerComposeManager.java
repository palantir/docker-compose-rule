/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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
import com.palantir.docker.compose.report.TestDescription;
import com.palantir.docker.compose.reporting.RunRecorder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
@CustomImmutablesStyle
public abstract class DockerComposeManager {
    private static final Logger log = LoggerFactory.getLogger(DockerComposeManager.class);

    public static final Duration DEFAULT_TIMEOUT = Duration.standardMinutes(2);
    public static final int DEFAULT_RETRY_ATTEMPTS = 2;

    private final RunRecorder runRecorder = RunRecorder.defaults();

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
    public com.palantir.docker.compose.execution.DockerCompose dockerCompose() {
        com.palantir.docker.compose.execution.DockerCompose
                dockerCompose = new DefaultDockerCompose(dockerComposeExecutable(), dockerExecutable(), machine());
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
        List<EventConsumer> eventConsumers =
                Stream.concat(Stream.of(runRecorder), eventConsumers().stream())
                .collect(Collectors.toList());

        return new EventEmitter(eventConsumers);
    }

    protected void setDescription(TestDescription testDescription) {
        runRecorder.setDescription(testDescription);
    }

    public void before() throws IOException, InterruptedException {
        log.debug("Starting docker-compose cluster");

        runRecorder.before(() -> dockerCompose().config());

        pullBuildAndUp();

        emitEventsFor().waitingForServices(this::waitForServices);
    }

    private void pullBuildAndUp() throws IOException, InterruptedException {
        if (pullOnStartup()) {
            emitEventsFor().pull(dockerCompose()::pull);
        }

        emitEventsFor().build(dockerCompose()::build);

        com.palantir.docker.compose.execution.DockerCompose upDockerCompose = dockerCompose();
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
        } finally {
            runRecorder.after();
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

    public interface BuilderExtensions<TSelf extends BuilderExtensions<TSelf>> {
        TSelf files(DockerComposeFiles files);

        TSelf logCollector(LogCollector logCollector);

        TSelf shutdownStrategy(ShutdownStrategy shutdownStrategy);

        TSelf addClusterWait(ClusterWait element);

        TSelf addAllClusterWaits(Iterable<? extends ClusterWait> elements);

        default TSelf file(String dockerComposeYmlFile) {
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
        default TSelf saveLogsTo(String path) {
            return logCollector(FileLogCollector.fromPath(path));
        }

        /**
         * Deprecated.
         * @deprecated Please use {@link DockerComposeManager#shutdownStrategy()} with {@link ShutdownStrategy#SKIP} instead.
         */
        @Deprecated
        default TSelf skipShutdown(boolean skipShutdown) {
            if (skipShutdown) {
                return shutdownStrategy(ShutdownStrategy.SKIP);
            }

            return (TSelf) this;
        }

        default TSelf waitingForService(String serviceName, HealthCheck<Container> healthCheck) {
            return waitingForService(serviceName, healthCheck, DEFAULT_TIMEOUT);
        }

        default TSelf waitingForService(String serviceName, HealthCheck<Container> healthCheck,
                ReadableDuration timeout) {
            ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(serviceName, healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        default TSelf waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck) {
            return waitingForServices(services, healthCheck, DEFAULT_TIMEOUT);
        }

        default TSelf waitingForServices(List<String> services, HealthCheck<List<Container>> healthCheck,
                ReadableDuration timeout) {
            ClusterHealthCheck clusterHealthCheck = serviceHealthCheck(services, healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        default TSelf waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck) {
            return waitingForHostNetworkedPort(port, healthCheck, DEFAULT_TIMEOUT);
        }

        default TSelf waitingForHostNetworkedPort(int port, HealthCheck<DockerPort> healthCheck,
                ReadableDuration timeout) {
            ClusterHealthCheck clusterHealthCheck = transformingHealthCheck(cluster -> new DockerPort(cluster.ip(), port, port), healthCheck);
            return addClusterWait(new ClusterWait(clusterHealthCheck, timeout));
        }

        default TSelf clusterWaits(Iterable<? extends ClusterWait> elements) {
            return addAllClusterWaits(elements);
        }
    }

    public static class Builder extends ImmutableDockerComposeManager.Builder implements BuilderExtensions<Builder> {
        @Override
        public DockerComposeManager build() {
            return super.build();
        }
    }

}
