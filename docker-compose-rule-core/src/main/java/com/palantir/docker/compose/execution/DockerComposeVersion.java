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
package com.palantir.docker.compose.execution;
import com.github.zafarkhaja.semver.Version;

public final class DockerComposeVersion {

    private DockerComposeVersion() {}

    //docker-compose version format is like 1.7.0rc1, which can't be parsed by java-semver
    //here we only pass 1.7.0 to java-semver
    public static Version parseFromDockerComposeVersion(String versionOutput) {
        String[] splitOnSeparator = versionOutput.split(" ");
        String version = splitOnSeparator[2];
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < version.length(); i++) {
            if (version.charAt(i) >= '0' && version.charAt(i) <= '9' || version.charAt(i) == '.') {
                builder.append(version.charAt(i));
            } else {
                return Version.valueOf(builder.toString());
            }
        }
        return Version.valueOf(builder.toString());
    }
}
