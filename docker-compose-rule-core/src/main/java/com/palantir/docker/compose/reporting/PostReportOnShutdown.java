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

import com.google.common.util.concurrent.Uninterruptibles;
import com.palantir.docker.compose.configuration.DockerComposeRuleConfig;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class PostReportOnShutdown {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Reporter REPORTER = createReporter();

    private static Reporter createReporter() {
        Optional<ReportingConfig> possibleReportingConfig =
                DockerComposeRuleConfig.findAutomatically().flatMap(DockerComposeRuleConfig::reporting);

        return possibleReportingConfig
                .<Reporter>map(reportingConfig -> new ReportCompiler(
                        Clock.systemUTC(),
                        reportingConfig.envVarWhitelistPatterns(),
                        new ReportPoster(new HttpJsonPoster(reportingConfig))::postReport))
                .orElse(Reporter.NoOpReporter.INSTANCE);
    }

    private PostReportOnShutdown() {}

    public static Reporter reporter() {
        ensureInstalled();
        return REPORTER;
    }

    @SuppressWarnings("ShutdownHook")
    private static void ensureInstalled() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            REPORTER.report();
            // Give time for logs to flush before killing
            Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        }));
    }
}
