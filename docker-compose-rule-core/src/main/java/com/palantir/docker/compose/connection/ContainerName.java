/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ContainerName {

    // Docker default container names have the format of:
    // <project>_<service>_<index> or <project>_<service>_<index>_<slug>
    // Regex without escape characters: ^(?<project>[^\W_]+)_(?<service>[^\W_]+)_(?<index>[^\W_]+)(_(?<slug>[^\W_]+))?$
    private static final Pattern DEFAULT_CONTAINER_NAME_PATTERN =
            Pattern.compile("^(?<project>[^\\W_]+)_(?<service>[^\\W_]+)_(?<index>[^\\W_]+)(_(?<slug>[^\\W_]+))?$");

    public abstract String rawName();

    public abstract String semanticName();

    @Override
    public String toString() {
        return semanticName();
    }

    public static ContainerName fromName(String name) {
        return ImmutableContainerName.builder()
                .rawName(name)
                .semanticName(parseSemanticName(name))
                .build();
    }

    private static String parseSemanticName(String name) {
        Matcher matcher = DEFAULT_CONTAINER_NAME_PATTERN.matcher(name);

        if (matcher.matches()) {
            return matcher.group("service");
        }

        return name;
    }
}
