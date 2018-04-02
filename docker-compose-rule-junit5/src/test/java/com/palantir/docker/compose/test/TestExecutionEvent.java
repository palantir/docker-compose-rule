package com.palantir.docker.compose.test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import org.immutables.value.Value;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;

/**
 * State information of an event triggered during the test execution lifecycle.
 *
 * @see https://github.com/junit-team/junit5/blob/master/junit-platform-engine/src/test/java/org/junit/platform/engine/test/event/ExecutionEvent.java
 */
@Value.Immutable
public abstract class TestExecutionEvent {

    private static final Supplier<IllegalArgumentException> EXCEPTION_NO_PAYLOAD = () ->
            new IllegalArgumentException("Cannot access payload from TestExecutionEvent when no payload is present.");

    private static TestExecutionEvent of(Type type, TestIdentifier identifier, Object payload) {
        return ImmutableTestExecutionEvent.builder()
                .type(type)
                .identifier(identifier)
                .rawPayload(payload)
                .build();
    }

    private static TestExecutionEvent of(Type type, TestIdentifier identifier) {
        return ImmutableTestExecutionEvent.builder()
                .type(type)
                .identifier(identifier)
                .build();
    }

    public static TestExecutionEvent reportingEntryPublished(TestIdentifier identifier, ReportEntry entry) {
        return of(Type.REPORTING_ENTRY_PUBLISHED, identifier, entry);
    }

    public static TestExecutionEvent dynamicTestRegistered(TestIdentifier identifier) {
        return of(Type.DYNAMIC_TEST_REGISTERED, identifier);
    }

    public static TestExecutionEvent executionStarted(TestIdentifier identifier) {
        return of(Type.EXECUTION_STARTED, identifier);
    }

    public static TestExecutionEvent executionFinished(TestIdentifier identifier, TestExecutionResult result) {
        return of(Type.EXECUTION_FINISHED, identifier, result);
    }

    public static TestExecutionEvent executionSkipped(TestIdentifier identifier, String reason) {
        return of(Type.EXECUTION_SKIPPED, identifier, reason);
    }

    @Value.Derived
    public LocalDateTime dateTimeOccurred() {
        return LocalDateTime.now();
    }

    @Value.Derived
    public boolean payloadIsReportEntry() {
        return rawPayload().map(object -> object instanceof ReportEntry).orElse(false);
    }

    public ReportEntry payloadAsReportEntry() {
        return payload(ReportEntry.class).orElseThrow(EXCEPTION_NO_PAYLOAD);
    }

    @Value.Derived
    public boolean payloadIsTestExecutionResult() {
        return rawPayload().map(object -> object instanceof TestExecutionResult).orElse(false);
    }

    public TestExecutionResult payloadAsTestExecutionResult() {
        return payload(TestExecutionResult.class).orElseThrow(EXCEPTION_NO_PAYLOAD);
    }

    @Value.Derived
    public boolean payloadIsString() {
        return rawPayload().map(object -> object instanceof String).orElse(false);
    }

    public String payloadAsString() {
        return payload(String.class).orElseThrow(EXCEPTION_NO_PAYLOAD);
    }

    public <T> Optional<T> payload(Class<T> expectedPayloadClass) {
        return rawPayload().map(expectedPayloadClass::cast);
    }

    public enum Type {
        DYNAMIC_TEST_REGISTERED,
        EXECUTION_SKIPPED,
        EXECUTION_STARTED,
        EXECUTION_FINISHED,
        REPORTING_ENTRY_PUBLISHED;
    }

    public abstract TestExecutionEvent.Type type();

    public abstract TestIdentifier identifier();

    protected abstract Optional<Object> rawPayload();

}
