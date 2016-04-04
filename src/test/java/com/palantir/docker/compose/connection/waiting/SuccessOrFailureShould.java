package com.palantir.docker.compose.connection.waiting;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SuccessOrFailureShould {
    @Test
    public void not_have_failed_if_actually_a_success() {
        assertThat(SuccessOrFailure.success().failed(), is(false));
    }

    @Test
    public void have_failed_if_actually_a_failure() {
        assertThat(SuccessOrFailure.failure("oops").failed(), is(true));
    }

    @Test
    public void return_the_failure_message_if_set() {
        assertThat(SuccessOrFailure.failure("oops").failureMessage(), is("oops"));
    }

    @Test(expected = RuntimeException.class)
    public void throw_an_exception_if_trying_to_get_the_failure_message_for_a_success() {
        SuccessOrFailure.success().failureMessage();
    }
}