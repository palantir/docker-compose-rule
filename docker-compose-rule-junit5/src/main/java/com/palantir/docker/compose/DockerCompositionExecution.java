package com.palantir.docker.compose;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DockerCompositionExecution implements DockerExecutionContext {

    public static DockerCompositionExecution of(DockerCompositionConfiguration configuration,
            LocalDateTime startupStartDateTime, LocalDateTime startupFinishDateTime) {
        return ImmutableDockerCompositionExecution.builder()
                .from(configuration)
                .startupStartDateTime(startupStartDateTime)
                .startupFinishDateTime(startupFinishDateTime)
                .executionConfiguration(configuration)
                .build();
    }

    @Value.Derived
    public Duration startupDuration() {
        return Duration.between(startupStartDateTime(), startupFinishDateTime());
    }

    @Value.Derived
    public LocalDateTime startDateTime() {
        return LocalDateTime.now();
    }

    public DockerCompositionConfiguration shutdown() throws IOException, InterruptedException {
        // Hard coded to the KILL_DOWN strategy
        ShutdownStrategy.KILL_DOWN.shutdown(dockerCompose(), docker());
        logCollector().stopCollecting();
        return executionConfiguration();
    }

    public DockerCompositionExecution restart() throws IOException, InterruptedException {
        return shutdown().startup();
    }

    public abstract LocalDateTime startupStartDateTime();

    public abstract LocalDateTime startupFinishDateTime();

    protected abstract DockerCompositionConfiguration executionConfiguration();

}
