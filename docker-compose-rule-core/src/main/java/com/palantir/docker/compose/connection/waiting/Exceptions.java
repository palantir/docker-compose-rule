package com.palantir.docker.compose.connection.waiting;

import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;

public enum Exceptions {
    ;

    public static String condensedStacktraceFor(Throwable throwable) {
        return ExceptionUtils.getThrowableList(throwable).stream()
                .map(t -> t.getClass().getCanonicalName() + ": " + t.getMessage())
                .collect(Collectors.joining("\n"));
    }
}
