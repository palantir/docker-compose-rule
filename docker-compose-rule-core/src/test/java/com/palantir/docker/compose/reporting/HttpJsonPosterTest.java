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

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

public class HttpJsonPosterTest {
    private final ReportingConfig reportingConfig = mock(ReportingConfig.class);
    private final HttpJsonPoster httpJsonPoster = new HttpJsonPoster(reportingConfig);

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule();

    @Test
    public void can_post_webhook() {
        wireMockRule.stubFor(post("/some/path").willReturn(status(200)));

        when(reportingConfig.url()).thenReturn(String.format("http://localhost:%s/some/path", wireMockRule.port()));

        String json = "{\"foo\":\"bar\"}";
        httpJsonPoster.post(json);

        wireMockRule.verify(postRequestedFor(urlPathEqualTo("/some/path")).withRequestBody(containing(json)));
    }
}
