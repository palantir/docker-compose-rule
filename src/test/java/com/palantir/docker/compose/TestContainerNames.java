/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose;

import static java.util.stream.Collectors.toList;

import com.palantir.docker.compose.connection.ContainerName;
import com.palantir.docker.compose.connection.ContainerNames;
import com.palantir.docker.compose.connection.ImmutableContainerName;
import java.util.Arrays;
import java.util.List;

public class TestContainerNames {

    private TestContainerNames() {}

    public static ContainerNames of(String... semanticNames) {
        List<ContainerName> testNames = Arrays.stream(semanticNames)
                .map(TestContainerNames::testContainerName)
                .collect(toList());
        return new ContainerNames(testNames);
    }

    private static ImmutableContainerName testContainerName(String testName) {
        return ImmutableContainerName.builder()
                .semanticName(testName)
                .rawName("123456_" + testName + "_1")
                .build();
    }

}
