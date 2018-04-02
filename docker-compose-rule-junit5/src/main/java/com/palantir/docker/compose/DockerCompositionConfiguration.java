package com.palantir.docker.compose;

import com.palantir.docker.compose.configuration.DaemonHostIpResolver;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ProjectName;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.ImmutableCluster;
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.execution.ConflictingContainerRemovingDockerCompose;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.execution.DockerExecutable;
import com.palantir.docker.compose.execution.RetryingDockerCompose;
import com.palantir.docker.compose.logging.DoNothingLogCollector;
import com.palantir.docker.compose.logging.FileLogCollector;
import com.palantir.docker.compose.logging.LogCollector;
import java.io.IOException;
import java.time.LocalDateTime;
import org.immutables.value.Value;
import org.joda.time.Duration;

@Value.Immutable
@CustomImmutablesStyle
public abstract class DockerCompositionConfiguration implements DockerExecutionContext {

    public static DockerCompositionConfiguration of(DockerComposition annotation) {
        return ImmutableDockerCompositionConfiguration.builder()
                .files(DockerComposeFiles.from(annotation.files()))
                .machine(determineDockerMachine(annotation.machineHost()))
                .projectName(determineProjectName(annotation.project()))
                .retryAttempts(annotation.retryAttempts())
                .nativeHealthCheckTimeout(Duration.standardSeconds(annotation.nativeHealthCheckTimeoutSeconds()))
                .logCollector(determineLogCollector(annotation.saveLogsToPath()))
                .removeConflictingContainersOnStartup(annotation.removeConflictingContainersOnStartup())
                .pullOnStartup(annotation.pullOnStartup())
                .tearDownBetweenTests(annotation.tearDownBetweenTests())
                .build();
    }

    private static ProjectName determineProjectName(String projectNameTemplate) {
        if (projectNameTemplate.equalsIgnoreCase(DockerComposition.TEMPLATE_RANDOM_PROJECT_NAME)) {
            return ProjectName.random();
        }
        return ProjectName.fromString(projectNameTemplate);
    }

    private static DockerMachine determineDockerMachine(String machineHost) {
        // TODO (cmoore): Should this be part of the DockerMachine class?
        if (machineHost.equalsIgnoreCase("localhost")
                || machineHost.equalsIgnoreCase(DaemonHostIpResolver.LOCALHOST)) {
            return DockerMachine.localMachine().build();
        }
        return DockerMachine.remoteMachine().host(machineHost).build();
    }

    private static LogCollector determineLogCollector(String logCollector) {
        return logCollector.equalsIgnoreCase(DockerComposition.TEMPLATE_NO_LOG_COLLECTOR)
                ? new DoNothingLogCollector()
                : FileLogCollector.fromPath(logCollector);
    }

    @Override
    @Value.Derived
    public DockerComposeExecutable dockerComposeExecutable() {
        return DockerComposeExecutable.builder()
                .dockerComposeFiles(files())
                .projectName(projectName())
                .dockerConfiguration(machine())
                .build();
    }

    @Override
    @Value.Derived
    public DockerExecutable dockerExecutable() {
        return DockerExecutable.builder()
                .dockerConfiguration(machine())
                .build();
    }

    @Override
    @Value.Derived
    public Docker docker() {
        return new Docker(dockerExecutable());
    }

    @Override
    @Value.Derived
    public DockerCompose dockerCompose() {
        DockerCompose defaultDockerCompose = new DefaultDockerCompose(dockerComposeExecutable(), machine());
        return removeConflictingContainersOnStartup()
                ? new ConflictingContainerRemovingDockerCompose(defaultDockerCompose, docker(), retryAttempts())
                : new RetryingDockerCompose(retryAttempts(), defaultDockerCompose);
    }

    @Override
    @Value.Derived
    public Cluster containers() {
        return ImmutableCluster.builder()
                .ip(machine().getIp())
                .containerCache(new ContainerCache(docker(), dockerCompose()))
                .build();
    }

    public DockerCompositionExecution startup() throws IOException, InterruptedException {
        LocalDateTime startupStart = LocalDateTime.now();
        if (pullOnStartup()) {
            dockerCompose().pull();
        }
        dockerCompose().build();
        dockerCompose().up();
        logCollector().startCollecting(dockerCompose());
        new ClusterWait(ClusterHealthCheck.nativeHealthChecks(), nativeHealthCheckTimeout())
                .waitUntilReady(containers());
        // TODO (cmoore): Implement handling for configured cluster waits
        LocalDateTime startupFinish = LocalDateTime.now();
        return DockerCompositionExecution.of(this, startupStart, startupFinish);
    }

    public abstract Duration nativeHealthCheckTimeout();

    public abstract boolean pullOnStartup();

    public abstract boolean tearDownBetweenTests();

    protected abstract int retryAttempts();

    protected abstract boolean removeConflictingContainersOnStartup();

}
