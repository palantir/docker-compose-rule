package com.palantir.docker.compose.test;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * Static utility class for running JUnit5 tests and reporting the execution results.
 */
public final class TestExecutor {

    private static final Launcher TEST_LAUNCHER = LauncherFactory.create();

    private TestExecutor() {}

    public static TestExecutionsReport execute(Class<?> testClass) {
        return execute(DiscoverySelectors.selectClass(testClass));
    }

    public static TestExecutionsReport execute(DiscoverySelector... selectors) {
        return execute(LauncherDiscoveryRequestBuilder.request()
                .selectors(selectors)
                .build());
    }

    public static TestExecutionsReport execute(LauncherDiscoveryRequest request) {
        TestExecutionReportRecorder reportRecorder = TestExecutionReportRecorder.create();
        TEST_LAUNCHER.execute(request, reportRecorder);
        return reportRecorder.toReport();
    }

}
