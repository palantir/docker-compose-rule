package com.palantir.docker.compose.utils;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class MockitoMultiAnswer<T> implements Answer<T> {
    private int numberOfTimesCalled = 0;

    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
        switch (numberOfTimesCalled++) {
            case 0: return firstCall(invocation);
            case 1: return secondCall(invocation);
            case 2: return thirdCall(invocation);
        }

        throw new RuntimeException("Called more times than supported");
    }

    protected T firstCall(InvocationOnMock invocation) throws Exception { throw new NotImplementedException(); }
    protected T secondCall(InvocationOnMock invocation) throws Exception { throw new NotImplementedException(); }
    protected T thirdCall(InvocationOnMock invocation) throws Exception { throw new NotImplementedException(); }
}