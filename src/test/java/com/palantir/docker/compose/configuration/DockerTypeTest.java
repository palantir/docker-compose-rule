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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import com.google.common.base.StandardSystemProperty;
import org.junit.Test;

public class DockerTypeTest {

    @Test
    public void testDefaultDockerTypeForMacIsRemote() {
        assumeTrue("Running on a non-Mac system; skipping Mac unit test",
                StandardSystemProperty.OS_NAME.value().startsWith(DockerType.MAC_OS));
        assertThat(DockerType.REMOTE, is(DockerType.getDefaultLocalDockerType()));
    }

    @Test
    public void testDefaultDockerTypeForNonMacIsDaemon() {
        assumeFalse("Running on a Mac system; skipping non-Mac unit test",
                StandardSystemProperty.OS_NAME.value().startsWith(DockerType.MAC_OS));
        assertThat(DockerType.DAEMON, is(DockerType.getDefaultLocalDockerType()));
    }

}
