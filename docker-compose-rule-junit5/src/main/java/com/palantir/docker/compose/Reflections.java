/*
 * Copyright 2018 Palantir Technologies, Inc. All rights reserved.
 */
package com.palantir.docker.compose;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Reflections {

    private Reflections() {}

    private static Predicate<Field> staticFields() {
        return field -> Modifier.isStatic(field.getModifiers());
    }

    private static Predicate<Field> fieldType(Class<?> type) {
        return field -> field.getType().isAssignableFrom(type);
    }

    public static List<Field> findStaticFieldsOfType(Class<?> onClass, Class<?> ofType) {
        return Stream.of(onClass.getDeclaredFields())
                .filter(staticFields())
                .filter(fieldType(ofType))
                .collect(Collectors.toList());
    }

}
