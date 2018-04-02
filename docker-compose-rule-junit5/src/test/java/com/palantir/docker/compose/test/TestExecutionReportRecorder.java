package com.palantir.docker.compose.test;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class TestExecutionReportRecorder implements TestExecutionListener {

    private TestExecutionsReport.Builder reportBuilder;

    private TestExecutionReportRecorder() {
        this.reportBuilder = TestExecutionsReport.builder();
    }

    public static TestExecutionReportRecorder create() {
        return new TestExecutionReportRecorder();
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        reportBuilder.testPlanStartState(testPlan);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        reportBuilder.testPlanFinishState(testPlan);
    }

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        reportBuilder.addExecutionEvents(TestExecutionEvent.dynamicTestRegistered(testIdentifier));
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        reportBuilder.addExecutionEvents(TestExecutionEvent.executionSkipped(testIdentifier, reason));
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        reportBuilder.addExecutionEvents(TestExecutionEvent.executionStarted(testIdentifier));
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        reportBuilder.addExecutionEvents(TestExecutionEvent.executionFinished(testIdentifier, testExecutionResult));
    }

    @Override
    public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        reportBuilder.addExecutionEvents(TestExecutionEvent.reportingEntryPublished(testIdentifier, entry));
    }

    public TestExecutionsReport toReport() {
        return reportBuilder.build();
    }

}
