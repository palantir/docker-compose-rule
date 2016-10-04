/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 */

package com.palantir.docker.compose;

import static java.util.stream.Collectors.toList;

import com.palantir.docker.compose.connection.ContainerName;
import com.palantir.docker.compose.connection.ImmutableContainerName;
import java.util.Arrays;
import java.util.List;

public class TestContainerNames {

    private TestContainerNames() {}

    public static List<ContainerName> of(String... semanticNames) {
        return Arrays.stream(semanticNames)
                .map(TestContainerNames::testContainerName)
                .collect(toList());
    }

    private static ContainerName testContainerName(String testName) {
        return ImmutableContainerName.builder()
                .semanticName(testName)
                .rawName("123456_" + testName + "_1")
                .build();
    }

}
