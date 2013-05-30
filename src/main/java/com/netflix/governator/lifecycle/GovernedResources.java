package com.netflix.governator.lifecycle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

public interface GovernedResources {
	Set<Class<?>> getClasses();

	Set<Constructor> getConstructors();

	Set<Method> getMethods();

	Set<Field> getFields();
}
