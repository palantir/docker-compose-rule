package com.palantir.docker.compose;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public abstract class ValueCachingMatcher<T> extends TypeSafeMatcher<T> {
    protected T value;

    @Override
    protected abstract void describeMismatchSafely(T item, Description mismatchDescription);

    @Override
    protected boolean matchesSafely(T t) {
        this.value = t;
        return matchesSafely();
    }

    protected abstract boolean matchesSafely();
}
