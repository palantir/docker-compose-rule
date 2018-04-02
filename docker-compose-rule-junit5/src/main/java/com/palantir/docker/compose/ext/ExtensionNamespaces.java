package com.palantir.docker.compose.ext;

import org.junit.jupiter.api.extension.ExtensionContext;

final class ExtensionNamespaces {

    private ExtensionNamespaces() {}

    static ExtensionContext.Namespace fromPackage(Package p) {
        return ExtensionContext.Namespace.create((Object[]) p.getName().split("\\."));
    }

}
