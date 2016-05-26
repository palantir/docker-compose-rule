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

import com.google.common.base.StandardSystemProperty;

public enum DockerType {

    DAEMON, REMOTE;

    public static final String MAC_OS = "Mac";

    public static DockerType getDefaultLocalDockerType() {
        if (StandardSystemProperty.OS_NAME.value().startsWith(MAC_OS)) {
            return REMOTE;
        } else {
            return DAEMON;
        }
    }

}
