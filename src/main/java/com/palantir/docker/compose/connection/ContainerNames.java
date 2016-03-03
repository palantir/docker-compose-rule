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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class ContainerNames implements Iterable<String> {

    private final List<String> containerNames;

    public ContainerNames(String singleContainerName) {
        this(singletonList(singleContainerName));
    }

    public ContainerNames(List<String> containerNames) {
        this.containerNames = containerNames;
    }

    public static ContainerNames parseFromDockerComposePs(String psOutput) {
        String[] splitOnSeparator = psOutput.split("-+\n");
        if (splitOnSeparator.length < 2) {
            return new ContainerNames(emptyList());
        }
        return new ContainerNames(getContainerNamesAtStartOfLines(splitOnSeparator[1]));
    }

    private static List<String> getContainerNamesAtStartOfLines(String psContainerOutput) {
        return Arrays.stream(psContainerOutput.split("\n"))
                     .map(String::trim)
                     .filter(line -> !line.isEmpty())
                     .map(line -> line.split(" "))
                     .map(psColumns -> psColumns[0])
                     .map(name -> name.split("_"))
                     .filter(nameComponents -> nameComponents.length == 3)
                     .map(nameComponents -> nameComponents[1])
                     .collect(toList());
    }

    @Override
    public Iterator<String> iterator() {
        return containerNames.iterator();
    }

    public Stream<String> stream() {
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
