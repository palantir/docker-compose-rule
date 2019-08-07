/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.docker.compose.reporting;

import com.google.common.annotations.VisibleForTesting;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.events.EventConsumer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.List;
import java.util.function.UnaryOperator;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class EnhancedDockerComposeRule {
    private EnhancedDockerComposeRule() {}

    public static final class Builder extends DockerComposeRule.Builder {
        private final EventConsumer eventConsumer;
        private final UnaryOperator<DockerComposeRule> wrapper;

        Builder(
                UnaryOperator<DockerComposeRule> wrapper,
                EventConsumer eventConsumer) {
            this.wrapper = wrapper;
            this.eventConsumer = eventConsumer;
        }

        @Override
        public DockerComposeRule build() {
            addEventConsumer(eventConsumer);
            return wrapper.apply(super.build());
        }
    }

    public static Builder builder() {
        return builder(Clock.systemUTC(), PostReportOnShutdown.reporter());
    }

    @VisibleForTesting
    static Builder builder(Clock clock, Reporter reporter) {
        RunRecorder runRecorder = new RunRecorder(clock, reporter);

        return new Builder(
                dockerComposeRule -> wrapDockerCompose(reporter, runRecorder, dockerComposeRule),
                runRecorder::addEvent);
    }

    private static DockerComposeRule wrapDockerCompose(
            Reporter reporter,
            RunRecorder runRecorder,
            DockerComposeRule dockerComposeRule) {
        // Here we us javassist to make a implementation of DockerComposeRule for us rather than implementing it
        // ourselves. In the long run, this is a more robust method of subclassing DockerComposeRule than extending
        // it, as if a new abstract method is added onto DCR (for another immutables parameter) then there will
        // effectively be an API break for the subclass, as it does not implement this new method. With javassist, we
        // can proxy all new methods through without changes. We need to have our implementation be a subclass of DCR
        // so that we can easily excavate out the change to use this library instead of DCR directly.

        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(DockerComposeRule.class);
        factory.setFilter(method -> true);

        Class clazz = factory.createClass();
        MethodHandler methodHandler = new MethodHandler() {
            @Override
            public Object invoke(
                    Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
                switch (thisMethod.getName()) {
                    case "apply":
                        return apply((Statement) args[0], (Description) args[1]);
                    case "before":
                        before();
                        return null;
                    case "after":
                        after();
                        return null;
                }
                return thisMethod.invoke(dockerComposeRule, args);
            }

            private Statement apply(Statement statement, Description description) {
                runRecorder.setDescription(description);

                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        try {
                            before();
                            statement.evaluate();
                        } finally {
                            after();
                        }
                    }
                };
            }

            private void before() throws IOException, InterruptedException {
                runRecorder.before(dockerComposeRule);
                dockerComposeRule.before();
            }

            private void after() {
                try {
                    dockerComposeRule.after();
                } finally {
                    runRecorder.after();
                }
            }
        };

        try {
            DockerComposeRule instrumentedDockerComposeRule =
                    (DockerComposeRule) clazz.getDeclaredConstructor().newInstance();
            ((Proxy) instrumentedDockerComposeRule).setHandler(methodHandler);
            return instrumentedDockerComposeRule;
        } catch (NoSuchMethodException
                | InvocationTargetException
                | InstantiationException
                | IllegalAccessException
                | RuntimeException e) {
            reporter.addException(e);
            throw new RuntimeException("Failed to construct " + EnhancedDockerComposeRule.class.getSimpleName(), e);
        }
    }
}
