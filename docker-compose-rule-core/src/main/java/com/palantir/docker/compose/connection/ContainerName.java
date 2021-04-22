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

import static java.util.stream.Collectors.joining;

import com.google.common.base.Splitter;
import java.util.Arrays;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@SuppressWarnings("checkstyle:DesignForExtension")
public abstract class ContainerName {

    public abstract String rawName();

    public abstract String semanticName();

    @Override
    public String toString() {
        return semanticName();
    }

    public static ContainerName fromPsLine(String psLine) {
        List<String> lineComponents = Splitter.on(" ").splitToList(psLine);
        String rawName = lineComponents.get(0);

        if (probablyCustomName(rawName)) {
            return ImmutableContainerName.builder()
                    .rawName(rawName)
                    .semanticName(rawName)
                    .build();
        }

        String semanticName = withoutDirectory(withoutScaleNumber(rawName));
        return ImmutableContainerName.builder()
                .rawName(rawName)
                .semanticName(semanticName)
                .build();
    }

    private static boolean probablyCustomName(String rawName) {
        return !(rawName.split("_").length >= 3);
    }

    private static String withoutDirectory(String rawName) {
        return Arrays.stream(rawName.split("_")).skip(1).collect(joining("_"));
    }

    public static String withoutScaleNumber(String rawName) {
        String[] components = rawName.split("_");
        return Arrays.stream(components).limit(components.length - 1).collect(joining("_"));
    }
}
