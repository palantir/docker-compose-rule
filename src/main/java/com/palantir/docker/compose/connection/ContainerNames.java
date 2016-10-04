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
import java.util.Objects;
import java.util.stream.Stream;

public class ContainerNames {

    private final List<ContainerName> containerNames;

    public static ContainerNames of(String... containerNames) {
        List<ContainerName> testNames = Arrays.stream(containerNames)
                .map(testName -> ImmutableContainerName.builder().semanticName(testName).rawName("123456_" + testName + "_1").build())
                .collect(toList());
        return new ContainerNames(testNames);
    }

    private ContainerNames(List<ContainerName> containerNames) {
        this.containerNames = containerNames;
    }

    public static ContainerNames parseFromDockerComposePs(String psOutput) {
        String[] splitOnSeparator = psOutput.split("-+\n");
        if (splitOnSeparator.length < 2) {
            return new ContainerNames(emptyList());
        }
        String psBody = splitOnSeparator[1];
        List<ContainerName> names = Arrays.stream(psBody.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(ContainerName::fromPsLine)
                .collect(toList());
        return new ContainerNames(names);
    }

    public Stream<ContainerName> stream() {
        return containerNames.stream();
    }

    public int size() {
        return containerNames.size();
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerNames);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContainerNames other = (ContainerNames) obj;
        return Objects.equals(containerNames, other.containerNames);
    }

    @Override
    public String toString() {
        return "ContainerNames [containerNames=" + containerNames + "]";
    }

}
