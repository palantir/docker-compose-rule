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
package com.palantir.docker.compose.matchers;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.connection.DockerMachine;
import java.util.Map;
import org.hamcrest.Description;

public final class DockerMachineEnvironmentMatcher extends ValueCachingMatcher<DockerMachine> {

    private final Map<String, String> expected;

    public DockerMachineEnvironmentMatcher(Map<String, String> expected) {
        this.expected = expected;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Docker Machine to have these environment variables:\n");
        description.appendValue(expected);
    }

    @Override
    protected boolean matchesSafely() {
        return missingEnvironmentVariables().isEmpty();
    }

    @Override
    protected void describeMismatchSafely(DockerMachine _item, Description mismatchDescription) {
        mismatchDescription.appendText("\nThese environment variables were missing:\n");
        mismatchDescription.appendValue(missingEnvironmentVariables());
    }

    public static DockerMachineEnvironmentMatcher containsEnvironment(Map<String, String> environment) {
        return new DockerMachineEnvironmentMatcher(ImmutableMap.copyOf(environment));
    }

    private Map<String, String> missingEnvironmentVariables() {
        Map<String, String> environment =
                value().configuredDockerComposeProcess().environment();
        return expected.entrySet().stream()
                .filter(required ->
                        !hasEntry(required.getKey(), required.getValue()).matches(environment))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
