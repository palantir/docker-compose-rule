package com.palantir.docker.compose.test;

import com.google.common.collect.ImmutableList;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

@Value.Immutable
public abstract class TestExecutionsReport {

    public static Builder builder() {
        return ImmutableTestExecutionsReport.builder();
    }

    public TestExecution firstExecution(String displayName, boolean caseInsensitive) {
        return executions().stream()
                .filter(execution -> caseInsensitive
                        ? execution.identifier().getDisplayName().equalsIgnoreCase(displayName)
                        : execution.identifier().getDisplayName().equals(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("No test execution found with display name: %s", displayName)));
    }

    public TestExecution firstExecution(String displayName) {
        return firstExecution(displayName, true);
    }

    @Value.Derived
    public List<TestExecution> executions() {
        ImmutableList.Builder<TestExecution> executions = ImmutableList.builder();
        Map<TestIdentifier, LocalDateTime> executionStartCache = new HashMap<>();
        for (TestExecutionEvent executionEvent : executionEvents()) {
            if (executionEvent.identifier().isTest()) {
                switch (executionEvent.type()) {
                    case EXECUTION_STARTED:
                        executionStartCache.put(executionEvent.identifier(), executionEvent.dateTimeOccurred());
                        continue;
                    case EXECUTION_SKIPPED:
                        // EXECUTION_SKIPPED will always have a String payload
                        executions.add(TestExecution.of(executionEvent.identifier(),
                                executionStartCache.get(executionEvent.identifier()),
                                executionEvent.dateTimeOccurred(),
                                executionEvent.payloadAsString()));
                        continue;
                    case EXECUTION_FINISHED:
                        // EXECUTION_FINISHED will always have a TestExecutionResult payload
                        executions.add(TestExecution.of(executionEvent.identifier(),
                                executionStartCache.get(executionEvent.identifier()),
                                executionEvent.dateTimeOccurred(),
                                executionEvent.payloadAsTestExecutionResult()));
                        continue;
                    default:
                }
            }
        }
        return executions.build();
    }

    public interface Builder {
        Builder testPlanStartState(TestPlan value);
        Builder testPlanFinishState(TestPlan value);
        Builder addExecutionEvents(TestExecutionEvent element);
        Builder addExecutionEvents(TestExecutionEvent... elements);
        Builder executionEvents(Iterable<? extends TestExecutionEvent> elements);
        Builder addAllExecutionEvents(Iterable<? extends TestExecutionEvent> elements);
        TestExecutionsReport build();
    }

    public abstract TestPlan testPlanStartState();

    public abstract TestPlan testPlanFinishState();

    protected abstract List<TestExecutionEvent> executionEvents();

}
