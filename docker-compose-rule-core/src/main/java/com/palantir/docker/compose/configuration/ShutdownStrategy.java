/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.docker.compose.configuration;

import com.palantir.docker.compose.execution.AggressiveShutdownStrategy;
import com.palantir.docker.compose.execution.AggressiveShutdownWithNetworkCleanupStrategy;
import com.palantir.docker.compose.execution.CallbackThenDelegateShutdownStrategy;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.GracefulShutdownStrategy;
import com.palantir.docker.compose.execution.KillDownShutdownStrategy;
import com.palantir.docker.compose.execution.SkipShutdownStrategy;
import java.io.IOException;

/**
 * How should a cluster of containers be shut down by the `after` method of
 * DockerComposeRule.
 */
@SuppressWarnings("ClassInitializationDeadlock")
public interface ShutdownStrategy {

    /**
     * Call rm on all containers, working around btrfs bug on CircleCI.
     *
     * @deprecated Use {@link #KILL_DOWN} (the default strategy)
     */
    @Deprecated
    ShutdownStrategy AGGRESSIVE = new AggressiveShutdownStrategy();
    /**
     * Call rm on all containers, then call docker-compose down.
     *
     * @deprecated Use {@link #KILL_DOWN} (the default strategy)
     */
    @Deprecated
    ShutdownStrategy AGGRESSIVE_WITH_NETWORK_CLEANUP = new AggressiveShutdownWithNetworkCleanupStrategy();
    /**
     * Call docker-compose down, kill, then rm. Allows containers up to 10 seconds to shut down
     * gracefully.
     *
     * <p>With this strategy, you will need to take care not to accidentally write images
     * that ignore their down signal, for instance by putting their run command in as a
     * string (which is interpreted by a SIGTERM-ignoring bash) rather than an array of strings.
     */
    ShutdownStrategy GRACEFUL = new GracefulShutdownStrategy();
    /**
     * Call docker-compose kill then down.
     */
    ShutdownStrategy KILL_DOWN = new KillDownShutdownStrategy();
    /**
     * Skip shutdown, leaving containers running after tests finish executing.
     *
     * <p>You can use this option to speed up repeated test execution locally by leaving
     * images up between runs. Do <b>not</b> commit it! You will be potentially abandoning
     * long-running processes and leaking resources on your CI platform!
     */
    ShutdownStrategy SKIP = new SkipShutdownStrategy();

    static ShutdownStrategy callbackAndThen(Runnable callback, ShutdownStrategy shutdownStrategy) {
        return new CallbackThenDelegateShutdownStrategy(shutdownStrategy, callback);
    }

    default void stop(DockerCompose _dockerCompose) throws IOException, InterruptedException {}

    void shutdown(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException;

}
