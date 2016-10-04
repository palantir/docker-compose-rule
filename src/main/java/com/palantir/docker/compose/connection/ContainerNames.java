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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

public class ContainerNames {

    private ContainerNames() {}

    public static List<ContainerName> parseFromDockerComposePs(String psOutput) {
        String[] splitOnSeparator = psOutput.split("-+\n");
        if (splitOnSeparator.length < 2) {
            return emptyList();
        }
        String psBody = splitOnSeparator[1];
        return Arrays.stream(psBody.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(ContainerName::fromPsLine)
                .collect(toList());
    }

}
