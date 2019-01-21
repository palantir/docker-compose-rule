/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.compose.logging;

import java.util.Optional;

public class LogDirectory {

    private LogDirectory() {}

    /**
     * For tests running on CircleCI, save logs into <code>$CIRCLE_ARTIFACTS/dockerLogs/&lt;testClassName&gt;</code>.
     * This ensures partial logs can be recovered if the build is cancelled or times out, and
     * also avoids needless copying.
     *
     * Otherwise, save logs from local runs to a folder inside <code>$project/build/dockerLogs</code> named
     * after the test class.
     *
     * @param testClass the JUnit test class whose name will appear on the log folder
     */
    public static String circleAwareLogDirectory(Class<?> testClass) {
        return circleAwareLogDirectory(testClass.getSimpleName());
    }

    public static String circleAwareLogDirectory(String logDirectoryName) {
        String artifactRoot = Optional.ofNullable(System.getenv("CIRCLE_ARTIFACTS")).orElse("build");
        return artifactRoot + "/dockerLogs/" + logDirectoryName;
    }

    /**
     * Save logs into a new folder, $project/build/dockerLogs/&lt;testClassName&gt;.
     */
    public static String gradleDockerLogsDirectory(Class<?> testClass) {
        return "build/dockerLogs/" + testClass.getSimpleName();
    }
}
