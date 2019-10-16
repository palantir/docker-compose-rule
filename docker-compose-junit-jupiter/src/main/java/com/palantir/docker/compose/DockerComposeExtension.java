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

import java.io.IOException;
import org.immutables.value.Value;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A JUnit 5 extension to bring up Docker containers defined in a docker-compose.yml before running tests.
 */
@Value.Immutable
@CustomImmutablesStyle
public abstract class DockerComposeExtension extends DockerComposeManager
        implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext unused) throws IOException, InterruptedException {
        try {
            before();
        } catch (RuntimeException e) {
            after();
            throw e;
        }
    }

    @Override
    public void afterAll(ExtensionContext unused) {
        after();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ImmutableDockerComposeExtension.Builder implements BuilderExtensions<Builder> {
        @Override
        public DockerComposeExtension build() {
            return super.build();
        }
    }
}
