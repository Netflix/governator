package com.netflix.governator.test.mock.spock;

import org.spockframework.mock.MockUtil;

import com.netflix.governator.guice.test.mocks.MockHandler;

import spock.lang.Specification;
import spock.mock.DetachedMockFactory;


public class SpockMockHandler implements MockHandler {
    
    private final DetachedMockFactory mock = new DetachedMockFactory();
    private final MockUtil mockUtil = new MockUtil();
    private Specification specification;
    
    @Override
    public <T> T createMock(Class<T> classToMock) {
         T mocked = mock.Mock(classToMock);
         mockUtil.attachMock(mocked, getSpecification());
         return mocked;
    }

    @Override
    public <T> T createMock(Class<T> classToMock, Object args) {
        return createMock(classToMock);
    }

    @Override
    public <T> T createSpy(T objectToSpy) {
         T spy = mock.Spy(objectToSpy);
         mockUtil.attachMock(spy, getSpecification());
         return spy;
    }

    @Override
    public void resetMock(Object mockToReset) {
        mockUtil.detachMock(mockToReset);
        mockUtil.attachMock(mockToReset, specification);
    }

    public Specification getSpecification() {
        return specification;
    }

    public void setSpecification(Specification specification) {
        this.specification = specification;
    }
}
