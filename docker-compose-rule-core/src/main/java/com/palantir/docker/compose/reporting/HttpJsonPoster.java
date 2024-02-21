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

import com.google.common.io.CharStreams;
import com.palantir.logsafe.logger.SafeLogger;
import com.palantir.logsafe.logger.SafeLoggerFactory;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

class HttpJsonPoster {
    private static final SafeLogger log = SafeLoggerFactory.get(HttpJsonPoster.class);

    private final ReportingConfig reportingConfig;

    HttpJsonPoster(ReportingConfig reportingConfig) {
        this.reportingConfig = reportingConfig;
    }

    public void post(String json) {
        try {
            URL url = new URL(reportingConfig.url());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(1_000);
            connection.setReadTimeout(10_000);

            connection.setRequestMethod("POST");
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String version = Optional.ofNullable(this.getClass().getPackage().getImplementationVersion())
                    .orElse("0.0.0");
            connection.setRequestProperty("User-Agent", "docker-compose-rule/" + version);

            connection.setDoOutput(true);
            PrintWriter body = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)));

            body.println(json);
            body.close();

            connection.connect();

            int status = connection.getResponseCode();

            if (status >= 400) {
                String error = CharStreams.toString(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));

                throw new RuntimeException("Posting json failed. Error is: " + error);
            }

            connection.disconnect();
        } catch (Exception e) {
            log.error("Failed to post report", e);
        }
    }
}
