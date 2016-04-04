package com.palantir.docker.compose.connection.waiting;

import org.junit.Test;

import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.failure;
import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.failureWithMessage;
import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.successful;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SuccessOrFailureShould {
    @Test
    public void not_have_failed_if_actually_a_success() {
        assertThat(SuccessOrFailure.success(), is(successful()));
    }

    @Test
    public void have_failed_if_actually_a_failure() {
        assertThat(SuccessOrFailure.failure("oops"), is(failure()));
    }

    @Test
    public void return_the_failure_message_if_set() {
        assertThat(SuccessOrFailure.failure("oops"), is(failureWithMessage("oops")));
    }
}