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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.CustomImmutablesStyle;
import java.util.regex.Pattern;
import org.immutables.value.Value;

@Value.Immutable
@CustomImmutablesStyle
@JsonDeserialize(as = ImmutableReportingConfig.class)
public interface ReportingConfig {
    String url();

    @Value.Auxiliary
    @Value.Derived
    default PatternCollection envVarWhitelistPatterns() {
        return new PatternCollection(ImmutableList.of(
                Pattern.compile("^CIRCLE")));
    }

    class Builder extends ImmutableReportingConfig.Builder {}

    static Builder builder() {
        return new Builder();
    }
}
