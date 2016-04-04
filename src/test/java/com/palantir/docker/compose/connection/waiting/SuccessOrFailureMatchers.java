package com.palantir.docker.compose.connection.waiting;

import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;

public enum SuccessOrFailureMatchers {;
    public static class Successful extends TypeSafeDiagnosingMatcher<SuccessOrFailure> {
        @Override
        protected boolean matchesSafely(SuccessOrFailure item, Description mismatchDescription) {
            if (item.failed()) {
                mismatchDescription.appendValue(item);
            }

            return item.succeeded();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is successful");
        }
    }

    public static Matcher<SuccessOrFailure> successful() {
        return new Successful();
    }

    public static class Failure extends FeatureMatcher<SuccessOrFailure, String> {
        public Failure(Matcher<? super String> subMatcher) {
            super(subMatcher, "failure message of", "failure message");
        }

        @Override
        protected String featureValueOf(SuccessOrFailure actual) {
            return actual.failureMessage();
        }

        @Override
        protected boolean matchesSafely(SuccessOrFailure actual, Description mismatch) {
            if (actual.succeeded()) {
                mismatch.appendValue(actual);
                return false;
            }

            return super.matchesSafely(actual, mismatch);
        }
    }

    public static Matcher<SuccessOrFailure> failure() {
        return new Failure(anything());
    }

    public static Matcher<SuccessOrFailure> failureWithMessage(Matcher<String> messageMatcher) {
        return new Failure(messageMatcher);
    }

    public static Matcher<SuccessOrFailure> failureWithMessage(String message) {
        return new Failure(equalTo(message));
    }
}
