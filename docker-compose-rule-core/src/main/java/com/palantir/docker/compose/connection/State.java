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
package com.palantir.docker.compose.connection;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum State {
    Up, Exit;

    private static final Pattern STATE_PATTERN = Pattern.compile("(Up|Exit)");
    private static final int STATE_INDEX = 1;

    public static State parseFromDockerComposePs(String psOutput) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(psOutput), "No container found");
        Matcher matcher = STATE_PATTERN.matcher(psOutput);
        Preconditions.checkState(matcher.find(), "Could not parse status: %s", psOutput);
        String matchedStatus = matcher.group(STATE_INDEX);
        return valueOf(matchedStatus);
    }
}
