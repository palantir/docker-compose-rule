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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.conjure.java.serialization.ObjectMappers;
import com.palantir.docker.compose.report.Report;

class ReportPoster {
    private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.newClientObjectMapper();

    private final WebhookPoster webhookPoster;

    ReportPoster(WebhookPoster webhookPoster) {
        this.webhookPoster = webhookPoster;
    }

    public void postReport(Report report) {
        webhookPoster.postHook(toJson(report));
    }

    private static String toJson(Report report) {
        try {
            return OBJECT_MAPPER.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
