/*
 * Copyright 2018 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.compose.ext;

import com.google.common.collect.Sets;
import com.palantir.docker.compose.DockerComposition;
import com.palantir.docker.compose.DockerCompositionConfiguration;
import com.palantir.docker.compose.DockerExecutionContext;
import com.palantir.docker.compose.connection.Container;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.util.AnnotationUtils;

public class DockerComposeExtension implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback,
        ExecutionCondition, ParameterResolver {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Optional<DockerCompositionConfiguration> executionConfig = findOrCreateExecutionContext(context);
        if (executionConfig.isPresent()) {
            
            DockerCompositionConfiguration execution = executionConfig.get();
            startUp(execution);
            updateExecutionInContext(execution, context);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Optional<DockerCompositionConfiguration> executionOptional = findExecutionContext(context);
        if (executionOptional.isPresent()) {
            DockerCompositionConfiguration execution = executionOptional.get();
            tearDown(execution);
            updateExecutionInContext(execution, context);
        }
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<DockerCompositionConfiguration> executionOptional = findExecutionContext(context);
        if (executionOptional.isPresent()) {
            DockerCompositionConfiguration execution = executionOptional.get();
            try {
                Set<Container> downContainers = Sets.newHashSet();
                for (Container container : execution.containers().allContainers()) {
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
            } catch (InterruptedException | IOException e) {
                return ConditionEvaluationResult.disabled(String.format("Unable to determine cluster state, "
                                + "skipping test execution '%s' due to error: %s",
                        context.getDisplayName(),
                        e.toString()));
            }
        }
        return ConditionEvaluationResult.enabled("No DockerComposition present, no checks required");
    }


    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Optional<DockerCompositionConfiguration> executionOptional = findExecutionContext(context);
        if (executionOptional.isPresent()) {
            DockerCompositionConfiguration execution = executionOptional.get();
            if (execution.tearDownBetweenTests()) {
                tearDown(execution);
                updateExecutionInContext(execution, context);
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Optional<DockerCompositionConfiguration> executionOptional = findExecutionContext(context);
        if (executionOptional.isPresent()) {
            DockerCompositionConfiguration execution = executionOptional.get();
            if (execution.tearDownBetweenTests()) {
                startUp(execution);
                updateExecutionInContext(execution, context);
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(DockerCompositionConfiguration.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return findDockerCompositionAnnotation(extensionContext)
                .flatMap(composition -> findExecution(composition, extensionContext))
                .orElseThrow(() ->
                        new IllegalStateException(String.format("Unable to inject DockerCompositionExtension into %s."
                                        + " This ",
                                extensionContext.getDisplayName())));
    }
}
