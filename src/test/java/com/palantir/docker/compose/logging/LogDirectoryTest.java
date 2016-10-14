/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class LogDirectoryTest {

    @Rule
    public final EnvironmentVariables variablesRule = new EnvironmentVariables();

    @Test
    public void gradleDockerLogsDirectory_should_use_class_simple_name() {
        String directory = LogDirectory.gradleDockerLogsDirectory(SomeTestClass.class);
        assertThat(directory, is("build/dockerLogs/SomeTestClass"));
    }

    @Test
    public void circleAwareLogDirectory_should_match_gradleDockerLogsDirectory_by_default() {
        variablesRule.set("CIRCLE_ARTIFACTS", null);
        String directory = LogDirectory.circleAwareLogDirectory(SomeTestClass.class);
        assertThat(directory, is("build/dockerLogs/SomeTestClass"));
    }

    @Test
    public void circleAwareLogDirectory_should_use_circle_environment_variable_if_available() {
        variablesRule.set("CIRCLE_ARTIFACTS", "/tmp/circle-artifacts.g4DjuuD");

        String directory = LogDirectory.circleAwareLogDirectory(SomeTestClass.class);
        assertThat(directory, is("/tmp/circle-artifacts.g4DjuuD/dockerLogs/SomeTestClass"));
    }

    private static class SomeTestClass {}
}
