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
package com.palantir.docker.compose.connection;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ContainerNames {
    private static final Pattern HEAD_PATTERN = Pattern.compile("-+(\r|\n)+");
    private static final Pattern BODY_PATTERN = Pattern.compile("(\r|\n)+");

    private ContainerNames() {}

    public static List<ContainerName> parseFromDockerComposePs(String psOutput) {
        List<String> psHeadAndBody = Splitter.on(HEAD_PATTERN).splitToList(psOutput);
        if (psHeadAndBody.size() < 2) {
            return emptyList();
        }

        String psBody = psHeadAndBody.get(1);
        return psBodyLines(psBody)
                .map(ContainerName::fromPsLine)
                .collect(toList());
    }

    private static Stream<String> psBodyLines(String psBody) {
        List<String> lines = Splitter.on(BODY_PATTERN).splitToList(psBody);
        return lines.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty());
    }

}
