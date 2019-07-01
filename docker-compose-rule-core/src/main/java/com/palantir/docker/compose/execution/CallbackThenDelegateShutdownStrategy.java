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

package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import java.io.IOException;

/**
 * Calls a supplied {@link Runnable} and then continues with shutdown. Could be useful for gathering logs or other
 * information before shutting down containers.
 */
public class CallbackThenDelegateShutdownStrategy implements ShutdownStrategy {

    private final ShutdownStrategy delegate;
    private final Runnable callback;

    public CallbackThenDelegateShutdownStrategy(ShutdownStrategy delegate, Runnable callback) {
        this.delegate = delegate;
        this.callback = callback;
    }

    @Override
    public void stop(DockerCompose dockerCompose) throws IOException, InterruptedException {
        try {
            callback.run();
        } finally {
            delegate.stop(dockerCompose);
        }
    }

    @Override
    public void down(DockerCompose dockerCompose) throws IOException, InterruptedException {
        delegate.down(dockerCompose);
    }
}
