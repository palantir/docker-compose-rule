/*
 * Copyright 2018 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.compose;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class ReflectionsTests {

    private static int staticOne = 1;
    private static String staticStringOne = "1";
    private static String staticStringTwo = "2";
    private int two = 2;

    @Test
    void testFindStaticFieldsOfType() {
        assertThat(Reflections.findStaticFieldsOfType(this.getClass(), int.class)
                .size(), is(1));
        assertThat(Reflections.findStaticFieldsOfType(this.getClass(), String.class)
                .size(), is(2));
    }

}
