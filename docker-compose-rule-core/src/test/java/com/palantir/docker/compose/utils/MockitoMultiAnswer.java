/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
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
package com.palantir.docker.compose.utils;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MockitoMultiAnswer<T> implements Answer<T> {
    private final List<Function<InvocationOnMock, T>> invocationHandlers;
    private int numberOfTimesCalled = 0;

    public MockitoMultiAnswer(List<Function<InvocationOnMock, T>> invocationHandlers) {
        this.invocationHandlers = ImmutableList.copyOf(invocationHandlers);
    }

    @SafeVarargs
    public static <T> MockitoMultiAnswer<T> of(Function<InvocationOnMock, T>... invocationHandlers) {
        return new MockitoMultiAnswer<>(Arrays.asList(invocationHandlers));
    }

    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
        if (numberOfTimesCalled >= invocationHandlers.size()) {
            throw new RuntimeException("Called more times than supported");
        }

        Function<InvocationOnMock, T> invocationHandler = invocationHandlers.get(numberOfTimesCalled);
        numberOfTimesCalled++;
        return invocationHandler.apply(invocation);
    }
}
