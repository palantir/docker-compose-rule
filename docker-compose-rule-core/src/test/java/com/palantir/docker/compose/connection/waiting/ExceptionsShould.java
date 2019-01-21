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
