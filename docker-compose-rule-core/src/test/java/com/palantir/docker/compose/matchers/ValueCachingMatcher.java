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
package com.palantir.docker.compose.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public abstract class ValueCachingMatcher<T> extends TypeSafeMatcher<T> {
    private T cachedValue;

    @Override
    protected abstract void describeMismatchSafely(T item, Description mismatchDescription);

    @Override
    protected boolean matchesSafely(T value) {
        cachedValue = value;
        return matchesSafely();
    }

    protected abstract boolean matchesSafely();

    public T value() {
        return cachedValue;
    }
}
