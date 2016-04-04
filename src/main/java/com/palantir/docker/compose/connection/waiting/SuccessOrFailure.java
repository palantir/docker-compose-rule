package com.palantir.docker.compose.connection.waiting;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
public abstract class SuccessOrFailure {
    @Value.Parameter protected abstract Optional<String> optionalFailureMessage();

    public static SuccessOrFailure success() {
        return ImmutableSuccessOrFailure.of(Optional.empty());
    }

    public static SuccessOrFailure failure(String message) {
        return ImmutableSuccessOrFailure.of(Optional.of(message));
    }

    public boolean failed() {
        return optionalFailureMessage().isPresent();
    }

    public boolean succeeded() {
        return !failed();
    }

    public Optional<String> failureMessage() {
        return optionalFailureMessage();
    }

    public static SuccessOrFailure fromBoolean(boolean succeeded, String possibleFailureMessage) {
        if (succeeded) {
            return success();
        } else {
            return failure(possibleFailureMessage);
        }
    }
}
