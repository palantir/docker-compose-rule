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

package com.palantir.docker.compose.instrumented;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.events.EventConsumer;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.junit.runner.Description;

public class InstrumentedDockerComposeRule {
    private final DockerComposeRule delegate;

    public InstrumentedDockerComposeRule(DockerComposeRule delegate) {
        this.delegate = delegate;
    }

    public static class Builder extends DockerComposeRule.Builder {
        private final EventConsumer eventConsumer;
        private final UnaryOperator<DockerComposeRule> wrapper;

        Builder(
                UnaryOperator<DockerComposeRule> wrapper,
                EventConsumer eventConsumer) {
            super();
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
        AtomicReference<Description> description = new AtomicReference<>();
        return new Builder(dockerComposeRule -> wrapDockerCompose(description, dockerComposeRule), events -> {
            System.out.println("description = " + description);
            System.out.println("events = " + events);
        });
    }

    private static DockerComposeRule wrapDockerCompose(
            AtomicReference<Description> savedDescription,
            DockerComposeRule dockerComposeRule) {
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
                        apply((Description) args[1]);
                        break;
                }
                return thisMethod.invoke(dockerComposeRule, args);
            }

            private void apply(Description description) {
                savedDescription.set(description);
            }
        };

        try {
            DockerComposeRule instrumentedDockerComposeRule = (DockerComposeRule) clazz.newInstance();
            ((Proxy) instrumentedDockerComposeRule).setHandler(methodHandler);
            return instrumentedDockerComposeRule;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
