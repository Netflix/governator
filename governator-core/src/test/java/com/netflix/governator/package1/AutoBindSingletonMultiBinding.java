package com.netflix.governator.package1;

import com.netflix.governator.annotations.AutoBindSingleton;

import javax.inject.Singleton;

@AutoBindSingleton(multiple=true, value=AutoBindSingletonInterface.class)
@Singleton
public class AutoBindSingletonMultiBinding implements AutoBindSingletonInterface {

}
