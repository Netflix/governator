package com.netflix.governator.guice.test.mocks;


/**
 * Abstraction for creating/resetting Mocks and Spies. Must have a default constructor.
 */
public interface MockHandler {

    <T> T createMock(Class<T> classToMock);

    <T> T createMock(Class<T> classToMock, Object answerMode);

    <T> T createSpy(T objectToSpy);

    void resetMock(Object mockToReset);
}
