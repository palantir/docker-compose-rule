package com.palantir.docker.compose.test;

import java.time.LocalDateTime;
import org.immutables.value.Value;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

@Value.Immutable
public abstract class TestExecution {

    public static TestExecution of(TestIdentifier identifier,
            LocalDateTime start, LocalDateTime end, String skipReason) {
        return ImmutableTestExecution.builder()
                .identifier(identifier)
                .start(start)
                .end(end)
                .resultOrSkipReason(TestExecutionResultOrSkipReason.of(skipReason))
                .build();
    }

    public static TestExecution of(TestIdentifier identifier,
            LocalDateTime start, LocalDateTime end, TestExecutionResult executionResult) {
        return ImmutableTestExecution.builder()
                .identifier(identifier)
                .start(start)
                .end(end)
                .resultOrSkipReason(TestExecutionResultOrSkipReason.of(executionResult))
                .build();
    }

    public abstract TestIdentifier identifier();

    public abstract LocalDateTime start();

    public abstract LocalDateTime end();

    public abstract TestExecutionResultOrSkipReason resultOrSkipReason();

}
