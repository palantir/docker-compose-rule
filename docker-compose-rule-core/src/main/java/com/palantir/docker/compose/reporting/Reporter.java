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

import com.palantir.docker.compose.configuration.DockerComposeRuleConfig;
import com.palantir.docker.compose.report.DockerComposeRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

interface Reporter {
    void addRun(DockerComposeRun dockerComposeRun);
    void addException(Exception exception);
    void report();

    final class NoOpReporter implements Reporter {
        private static final Logger log = LoggerFactory.getLogger(NoOpReporter.class);

        public static final Reporter INSTANCE = new NoOpReporter();

        private NoOpReporter() { }

        @Override
        public void addRun(DockerComposeRun _dockerComposeRun) {
        }

        @Override
        public void addException(Exception _exception) {
        }

        @Override
        public void report() {
            log.debug("Not posting report as no " + DockerComposeRuleConfig.CONFIG_FILENAME + " config file found");
        }
    }
}
