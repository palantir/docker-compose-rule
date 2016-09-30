/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
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
package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.connection.ContainerNames;
import java.io.IOException;

public class RetryingDockerCompose extends DelegatingDockerCompose {
    private final Retryer retryer;

    public RetryingDockerCompose(int retryAttempts, DockerCompose dockerCompose) {
        this(new Retryer(retryAttempts, Retryer.STANDARD_DELAY), dockerCompose);
    }

    public RetryingDockerCompose(Retryer retryer, DockerCompose dockerCompose) {
        super(dockerCompose);
        this.retryer = retryer;
    }

    @Override
    public void up() throws IOException, InterruptedException {
        retryer.<Void>runWithRetries(() -> {
            super.up();
            return null;
        });
    }

    @Override
    public ContainerNames ps() throws IOException, InterruptedException {
        return retryer.runWithRetries(super::ps);
    }
}
