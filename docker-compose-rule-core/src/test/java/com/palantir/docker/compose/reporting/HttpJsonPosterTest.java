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

import org.junit.Test;

public class HttpJsonPosterTest {
    private final HttpJsonPoster httpJsonPoster = new HttpJsonPoster(ReportingConfig.builder()
            .url("https://papaya-webhook-receiver.palantir.build/api/general/enhanced-docker-compose-rule-testing/hook")
            .build());

    @Test
    public void can_post_webhook() {
        String json = "{\"foo\":\"bar\"}";
        httpJsonPoster.post(json);
    }

}
