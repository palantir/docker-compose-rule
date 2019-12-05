/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.docker.compose.connection;


import com.google.common.base.Splitter;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ContainerName {

    // Containers can be given custom names within docker-compose.yml
    // Or by default will be assigned a name like:
    // * <project>_<service>_<index> (docker-compose version < 1.23.0)
    // * <project>_<service>_<index>_<slug> (docker-compose version >= 1.23.0)

    // The assigned name of the container
    public abstract String rawName();

    // Custom name if container was named, otherwise simply the name
    // of the service stripped from the default name
    public abstract String semanticName();

    @Override
    public String toString() {
        return semanticName();
    }

    public static ContainerName fromName(String name) {
        return ImmutableContainerName.builder()
                .rawName(name)
                .semanticName(isProbablyDefaultName(name)
                        ? stripSemanticNameFromDefaultName(name)
                        : name)
                .build();
    }

    private static boolean isProbablyDefaultName(String name) {
        return Splitter.on("_").splitToList(name).size() >= 3;
    }

    private static String stripSemanticNameFromDefaultName(String name) {
        return Splitter.on("_").splitToList(name).get(1); // Technically a break
    }
}
