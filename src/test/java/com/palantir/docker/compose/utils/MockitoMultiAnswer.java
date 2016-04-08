package com.palantir.docker.compose.utils;

import com.google.common.collect.ImmutableList;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class MockitoMultiAnswer<T> implements Answer<T> {
    private final List<Function<InvocationOnMock, T>> invocationHandlers;
    private int numberOfTimesCalled = 0;

    public MockitoMultiAnswer(List<Function<InvocationOnMock, T>> invocationHandlers) {
        this.invocationHandlers = ImmutableList.copyOf(invocationHandlers);
    }

    public static <T> MockitoMultiAnswer<T> of(Function<InvocationOnMock, T>... invocationHandlers) {
        return new MockitoMultiAnswer<>(Arrays.asList(invocationHandlers));
    }

    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
        if (numberOfTimesCalled >= invocationHandlers.size()) {
            throw new RuntimeException("Called more times than supported");
        }

        Function<InvocationOnMock,T> invocationHandler = invocationHandlers.get(numberOfTimesCalled);
        numberOfTimesCalled++;
        return invocationHandler.apply(invocation);
    }
}