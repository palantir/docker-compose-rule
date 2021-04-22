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

package com.palantir.docker.compose.execution;

import com.palantir.docker.compose.configuration.ShutdownStrategy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send SIGTERM to containers first, allowing them up to 10 seconds to
 * terminate before killing and rm-ing them.
 */
public class GracefulShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownStrategy.class);

    @Override
    public void stop(DockerCompose dockerCompose) throws IOException, InterruptedException {
        log.debug("Stopping docker-compose cluster");
        dockerCompose.stop();
        dockerCompose.kill();
    }

    @Override
    public void shutdown(DockerCompose dockerCompose, Docker _docker) throws IOException, InterruptedException {
        log.debug("Downing docker-compose cluster");
        dockerCompose.down();
    }
}
