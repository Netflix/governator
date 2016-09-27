package com.netflix.governator.providers;

import java.util.function.Function;

public interface ProvisionAdvice<T> extends Function<T, T> {
}
