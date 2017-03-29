package com.netflix.governator.guice.test.mocks.mockito;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.netflix.governator.guice.test.mocks.MockHandler;

public class MockitoMockHandler implements MockHandler {

    @Override
    public <T> T createMock(Class<T> classToMock) {
        return Mockito.mock(classToMock);
    }

    @Override
    public <T> T createMock(Class<T> classToMock, Object args) {
        if (args instanceof Answer<?>) {
            return Mockito.mock(classToMock, (Answer<?>) args);
        } else {
            throw new IllegalArgumentException(
                    "MockitoMockHandler only supports arguments of type " + Answer.class.getName() + ". Provided " + args != null
                            ? args.getClass().getName() : "null");
        }
    }

    @Override
    public <T> T createSpy(T objectToSpy) {
        return Mockito.spy(objectToSpy);
    }

    @Override
    public void resetMock(Object mockToReset) {
        Mockito.reset(mockToReset);
    }

}
