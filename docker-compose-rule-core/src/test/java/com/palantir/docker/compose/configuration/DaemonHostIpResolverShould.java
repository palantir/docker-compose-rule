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
package com.palantir.docker.compose.configuration;

import static com.palantir.docker.compose.configuration.DaemonHostIpResolver.LOCALHOST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class DaemonHostIpResolverShould {

    @Test
    public void return_local_host_with_null() throws Exception {
        assertThat(new DaemonHostIpResolver().resolveIp(null), is(LOCALHOST));
    }

    @Test
    public void return_local_host_with_blank() throws Exception {
        assertThat(new DaemonHostIpResolver().resolveIp(""), is(LOCALHOST));
    }

    @Test
    public void return_local_host_with_arbitrary() throws Exception {
        assertThat(new DaemonHostIpResolver().resolveIp("arbitrary"), is(LOCALHOST));
    }

}
