package com.palantir.docker.compose.connection.waiting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class ExceptionsShould {
    @Test
    public void print_out_a_condensed_version_of_the_stacktrace() {
        RuntimeException exception = new RuntimeException("foo", new IllegalStateException("bar", new UnsupportedOperationException("baz")));
        assertThat(Exceptions.condensedStacktraceFor(exception), is(
                  "java.lang.RuntimeException: foo\n"
                + "java.lang.IllegalStateException: bar\n"
                + "java.lang.UnsupportedOperationException: baz"
        ));
    }
}