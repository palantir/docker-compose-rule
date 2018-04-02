package com.palantir.docker.compose.ext;

import com.palantir.docker.compose.DockerComposition;
import com.palantir.docker.compose.DockerCompositionConfiguration;
import com.palantir.docker.compose.DockerExecutionContext;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

public final class DockerComposeExtensions {

    private DockerComposeExtensions() {}

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionNamespaces.fromPackage(DockerComposeExtensions.class.getPackage());

    private static Function<ExtensionContext, Optional<DockerComposition>> FIND_ANNOTATION = context ->
            AnnotationUtils.findAnnotation(context.getElement(), DockerComposition.class);

    public static Optional<DockerExecutionContext> findOrCreateExecutionContext(ExtensionContext context) {
        return FIND_ANNOTATION.apply(context).map(annotation ->
                context.getStore(NAMESPACE).getOrComputeIfAbsent(annotation.project(), ignored ->
                        DockerCompositionConfiguration.of(annotation), DockerCompositionConfiguration.class));
    }

    public static Optional<DockerExecutionContext> findExecutionContext(ExtensionContext context) {
        return FIND_ANNOTATION.apply(context).map(annotation ->
                context.getStore(NAMESPACE).get(annotation.project(), DockerExecutionContext.class));
    }

    public static void updateExecutionInContext(DockerCompositionConfiguration execution,
            ExtensionContext extensionContext) {
        extensionContext.getStore(NAMESPACE).put(execution.projectName().asString(), execution);
    }

}
