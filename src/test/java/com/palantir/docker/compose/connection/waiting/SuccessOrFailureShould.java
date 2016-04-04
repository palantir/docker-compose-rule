package com.palantir.docker.compose.connection.waiting;

import org.junit.Test;

import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.failure;
import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.failureWithMessage;
import static com.palantir.docker.compose.connection.waiting.SuccessOrFailureMatchers.successful;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
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

    @Test
    public void fail_from_an_exception() {
        Exception exception = new RuntimeException("oh no");
        assertThat(SuccessOrFailure.fromException(exception),
            is(failureWithMessage(both(
                containsString("RuntimeException")).and(
                containsString("oh no")
            ))));
    }
}