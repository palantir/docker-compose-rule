/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.palantir.docker.compose.connection.Container;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultiServiceHealthCheckShould {

    private static final Container CONTAINER = mock(Container.class);
    private static final Container OTHER_CONTAINER = mock(Container.class);

    private final SingleServiceHealthCheck delegate = mock(SingleServiceHealthCheck.class);
    private final MultiServiceHealthCheck healthCheck = MultiServiceHealthCheck.fromSingleServiceHealthCheck(delegate);

    @Test public void
    delegate_to_the_wrapped_single_service_health_check() {
        when(delegate.isServiceUp(CONTAINER)).thenReturn(SuccessOrFailure.success());

        assertThat(
                healthCheck.areServicesUp(ImmutableList.of(CONTAINER)),
                is(delegate.isServiceUp(CONTAINER)));
    }

    @Test(expected = IllegalArgumentException.class) public void
    throw_an_error_when_a_wrapped_health_check_is_passed_more_than_1_argument() {
        healthCheck.areServicesUp(ImmutableList.of(CONTAINER, OTHER_CONTAINER));
    }

    @Test(expected = IllegalArgumentException.class) public void
    throw_an_error_when_a_wrapped_health_check_is_passed_0_arguments() {
        healthCheck.areServicesUp(ImmutableList.of(CONTAINER, OTHER_CONTAINER));
    }

}
