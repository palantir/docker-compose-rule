package com.palantir.docker.compose.test;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import java.util.function.Supplier;
import org.immutables.value.Value;
import org.junit.platform.engine.TestExecutionResult;

/**
 * Union type that allows propagation of terminated state of a test,
 * supporting the reason {@link String} if the test was skipped,
 * or the {@link TestExecutionResult} if the test was executed.
 */
@Value.Immutable
public abstract class TestExecutionResultOrSkipReason {

    private static final Supplier<UnsupportedOperationException> NOT_EXECUTION_RESULT = () ->
            new UnsupportedOperationException("No execution result contained, "
                    + "this is a skip reason.");

    private static final Supplier<UnsupportedOperationException> NOT_SKIP_REASON = () ->
            new UnsupportedOperationException("No skip reason contained,"
                    + " this is an execution result.");

    public static TestExecutionResultOrSkipReason of(String skipReason) {
        return ImmutableTestExecutionResultOrSkipReason.builder()
                .maybeSkipReason(skipReason)
                .build();
    }

    public static TestExecutionResultOrSkipReason of(TestExecutionResult executionResult) {
        return ImmutableTestExecutionResultOrSkipReason.builder()
                .maybeExecutionResult(executionResult)
                .build();
    }

    @Value.Check
    protected void check() {
        checkArgument(maybeExecutionResult().isPresent()
                        || maybeSkipReason().isPresent(),
                "Must have either an executionResult or a skipReason.");
    }

    @Value.Derived
    public boolean isSkipReason() {
        return maybeSkipReason().isPresent();
    }

    @Value.Derived
    public boolean isExecutionResult() {
        return maybeExecutionResult().isPresent();
    }

    public TestExecutionResult executionResult() {
        return maybeExecutionResult().orElseThrow(NOT_EXECUTION_RESULT);
    }

    public String skipReason() {
        return maybeSkipReason().orElseThrow(NOT_SKIP_REASON);
    }

    protected abstract Optional<TestExecutionResult> maybeExecutionResult();

    protected abstract Optional<String> maybeSkipReason();

}
