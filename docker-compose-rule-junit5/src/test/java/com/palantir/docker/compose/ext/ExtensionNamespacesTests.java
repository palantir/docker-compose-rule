package com.palantir.docker.compose.ext;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.docker.compose.ext.ExtensionNamespaces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

class ExtensionNamespacesTests {

    @Test
    void testFromPackage() {
        assertThat(ExtensionNamespaces.fromPackage(getClass().getPackage()))
                .isEqualTo(ExtensionContext.Namespace.create("com", "palantir", "docker", "compose"));
    }

}
