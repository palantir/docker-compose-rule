/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.docker.compose.helpers;

import static com.google.common.base.Preconditions.checkState;

import com.github.zafarkhaja.semver.Version;
import com.google.common.base.Joiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * We depend on semantic versioning (https://semver.org/) and use Java SemVer within
 * this library to handle versions (https://github.com/zafarkhaja/jsemver).
 * Docker however doesn't use exact semantic versioning so this helper does
 * some light processing to get simple, useable versions
 */
public enum VersionHelper {
    INSTANCE;

    // Semantic versions have a structure of <major>.<minor>.<patch>-<pre-release>+<build metadata>
    // This regex groups pre-release and build metadata into the "rest" group
    // Regex without escape characters: ^(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)(?<rest>.*)$
    private static final Pattern BASIC_SEM_VER_PATTERN =
            Pattern.compile("^(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)(?<rest>.*)$");

    public static Version toSemVer(String version) {
        Matcher matcher = BASIC_SEM_VER_PATTERN.matcher(version);

        checkState(matcher.matches(), "Unexpected version structure: %s", version);

        String major = dropLeadingZeroes(matcher.group("major"));
        String minor = dropLeadingZeroes(matcher.group("minor"));
        String patch = dropLeadingZeroes(matcher.group("patch"));

        String versionCore = Joiner.on(".").join(major, minor, patch);

        return Version.valueOf(versionCore);
    }

    private static String dropLeadingZeroes(String numericString) {
        return String.valueOf(Integer.parseInt(numericString));
    }
}
