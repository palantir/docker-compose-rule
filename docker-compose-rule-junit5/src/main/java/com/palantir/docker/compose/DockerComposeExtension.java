/*
 * Copyright 2018 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.compose;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Sets;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.execution.ConflictingContainerRemovingDockerCompose;
import com.palantir.docker.compose.execution.DockerCompose;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerComposeExtension implements BeforeAllCallback, AfterAllCallback,
        ExecutionCondition {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeExtension.class);

    private static DockerComposeRule getAsDockerComposeRule(Field dockerComposeRuleField) {
        try {
            // Double check for static field since we are passing null to Field.get(null);
            checkArgument(Modifier.isStatic(dockerComposeRuleField.getModifiers()),
                    "dockerComposeRuleField must be static");
            return DockerComposeRule.class.cast(dockerComposeRuleField.get(null));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        DockerComposeRule composeRule = findDockerComposeRule(context);
        log.debug("Starting docker-compose cluster");
        if (composeRule.pullOnStartup()) {
            composeRule.dockerCompose().pull();
        }

        composeRule.dockerCompose().build();

        DockerCompose upDockerCompose = composeRule.dockerCompose();
        if (composeRule.removeConflictingContainersOnStartup()) {
            upDockerCompose = new ConflictingContainerRemovingDockerCompose(upDockerCompose, composeRule.docker());
        }
        upDockerCompose.up();

        composeRule.logCollector().startCollecting(composeRule.dockerCompose());
        log.debug("Waiting for services");
        new ClusterWait(ClusterHealthCheck.nativeHealthChecks(),
                composeRule.nativeServiceHealthCheckTimeout())
                .waitUntilReady(composeRule.containers());
        log.debug("docker-compose cluster started");
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        DockerComposeRule composeRule = findDockerComposeRule(context);
        composeRule.shutdownStrategy().shutdown(composeRule.dockerCompose(), composeRule.docker());
        composeRule.logCollector().stopCollecting();
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        DockerComposeRule composeRule = findDockerComposeRule(context);
        try {
            Set<Container> downContainers = Sets.newHashSet();
            for (Container container : composeRule.containers().allContainers()) {
                if (!container.state().isUp()) {
                    downContainers.add(container);
                }
            }
            if (downContainers.isEmpty()) {
                return ConditionEvaluationResult.enabled("Docker cluster is up");
            } else {
                return ConditionEvaluationResult.disabled(
                        String.format("Skipping test execution due to containers being down: %s", downContainers));
            }
        } catch (IOException | InterruptedException e) {
            return ConditionEvaluationResult.disabled(
                    String.format("Unable to determine cluster state, skipping test execution '%s' due to error: %s",
                            context.getDisplayName(), e.toString()));
        }
    }

    private DockerComposeRule findDockerComposeRule(ExtensionContext context) {
        Class<?> testClass = context.getTestClass()
                .orElseThrow(() -> new IllegalStateException(
                        "DockerComposeExtension is required to be applied at the class level"));
        List<Field> declaredDockerComposeRules =
                Reflections.findStaticFieldsOfType(testClass, DockerComposeRule.class);
        if (declaredDockerComposeRules.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Static DockerComposeRule must be declared within '%s' but none were found",
                    testClass.getName()));
        } else if (declaredDockerComposeRules.size() > 1) {
            throw new IllegalStateException(String.format(
                    "Multiple DockerComposeRules found to be declared within '%s', only one should be declared",
                    testClass.getName()));
        }
        return getAsDockerComposeRule(declaredDockerComposeRules.get(0));
    }

}
