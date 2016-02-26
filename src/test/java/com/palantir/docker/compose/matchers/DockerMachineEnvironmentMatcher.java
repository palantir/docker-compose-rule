package com.palantir.docker.compose.matchers;

import static java.util.stream.Collectors.toMap;

import static org.hamcrest.collection.IsMapContaining.hasEntry;

import java.util.Map;

import org.hamcrest.Description;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.connection.DockerMachine;

public class DockerMachineEnvironmentMatcher extends ValueCachingMatcher<DockerMachine> {

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
    protected void describeMismatchSafely(DockerMachine item, Description mismatchDescription) {
        mismatchDescription.appendText("\nThese environment variables were missing:\n");
        mismatchDescription.appendValue(missingEnvironmentVariables());
    }

    public static DockerMachineEnvironmentMatcher containsEnvironment(Map<String, String> environment) {
        return new DockerMachineEnvironmentMatcher(ImmutableMap.copyOf(environment));
    }

    private Map<String, String> missingEnvironmentVariables() {
        Map<String, String> environment = value.configDockerComposeProcess()
                                               .environment();
        return expected.entrySet()
                       .stream()
                       .filter(required -> !hasEntry(required.getKey(), required.getValue()).matches(environment))
                       .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
