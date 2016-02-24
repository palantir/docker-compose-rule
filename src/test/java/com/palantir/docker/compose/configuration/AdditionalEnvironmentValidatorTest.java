package com.palantir.docker.compose.configuration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;

public class AdditionalEnvironmentValidatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void additionalEnvironmentVariablesWithDockerVariables() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder().put("DOCKER_HOST", "tcp://some-host:2376")
                                                                              .put("SOME_VARIABLE" , "Some Value")
                                                                              .build();
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The following variables");
        exception.expectMessage("DOCKER_HOST");
        exception.expectMessage("cannot exist in your additional environment");
        AdditionalEnvironmentValidator.INSTANCE.validate(variables);
    }

    @Test
    public void validArbitraryEnvironmentVariables() throws Exception {
        Map<String, String> variables = ImmutableMap.<String, String>builder().put("SOME_VARIABLE" , "Some Value")
                                                                              .build();

        assertThat(AdditionalEnvironmentValidator.INSTANCE.validate(variables), is(variables));
    }
}