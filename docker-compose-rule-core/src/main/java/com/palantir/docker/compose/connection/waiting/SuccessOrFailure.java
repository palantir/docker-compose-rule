/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.docker.compose.connection.waiting;

import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.immutables.value.Value;

@Value.Immutable
public abstract class SuccessOrFailure {
    public static SuccessOrFailure onResultOf(Attempt attempt) {
        try {
            return fromBoolean(attempt.attempt(), "Attempt to complete healthcheck failed");
        } catch (Exception e) {
            return fromException(e);
        }
    }

    public SuccessOrFailure mapFailure(Function<String, String> mapper) {
        if (this.succeeded()) {
            return this;
        } else {
            return failure(mapper.apply(failureMessage()));
        }
    }

    @Value.Parameter protected abstract Optional<String> optionalFailureMessage();

    public static SuccessOrFailure success() {
        return ImmutableSuccessOrFailure.of(Optional.empty());
    }

    public static SuccessOrFailure failure(String message) {
        return ImmutableSuccessOrFailure.of(Optional.of(message));
    }

    public static SuccessOrFailure failureWithCondensedException(String message, Exception exception) {
        return failure(message + ":\n" + Exceptions.condensedStacktraceFor(exception));
    }

    public static SuccessOrFailure fromBoolean(boolean succeeded, String possibleFailureMessage) {
        if (succeeded) {
            return success();
        } else {
            return failure(possibleFailureMessage);
        }
    }

    public boolean failed() {
        return optionalFailureMessage().isPresent();
    }

    public boolean succeeded() {
        return !failed();
    }

    public String failureMessage() {
        return optionalFailureMessage().get();
    }

    public Optional<String> toOptionalFailureMessage() {
        return optionalFailureMessage();
    }

    public static SuccessOrFailure fromException(Exception exception) {
        return SuccessOrFailure.failure("Encountered an exception: " + ExceptionUtils.getStackTrace(exception));
    }
}
