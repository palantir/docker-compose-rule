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

package com.palantir.docker.compose;

import com.palantir.docker.compose.report.TestDescription;
import java.util.Optional;
import org.immutables.value.Value;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@Value.Immutable
@CustomImmutablesStyle
@SuppressWarnings("DesignForExtension")
public abstract class DockerComposeRule extends DockerComposeManager implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        this.setDescription(TestDescription.builder()
                .testClass(Optional.ofNullable(description.getClassName()))
                .displayName(Optional.ofNullable(description.getDisplayName()))
                .method(Optional.ofNullable(description.getMethodName()))
                .build());

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    before();
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ImmutableDockerComposeRule.Builder implements BuilderExtensions<Builder> {
        @Override
        public DockerComposeRule build() {
            return super.build();
        }
    }
}
