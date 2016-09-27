package com.netflix.governator.providers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import org.junit.Test;

import javax.inject.Named;
import javax.inject.Singleton;

public class AdvisedProviderTest {
    @Test
    public void test1() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(AdvisableAnnotatedMethodScanner.asModule());
            }
            
            @ProvidesWithAdvice
            @Singleton
            String getString() {
                return "bar";
            }
            
            @ProvidesWithAdvice
            @Singleton
            @Named("foo")
            String getFoo() {
                return "foo";
            }
            
            @Advice
            @Order(5)
            ProvisionAdvice<String> adviseFoo1() {
                return str -> str += "-advised1";
            }
            
            @Advice
            @Order(5)
            ProvisionAdvice<String> adviseFoo12() {
                return str -> str += "-advised12";
            }
            
            @Advice
            @Order(2)
            ProvisionAdvice<String> adviseFoo2() {
                return str -> str += "-advised2";
            }
            
            @Advice
            @Order(2)
            @Named("foo")
            ProvisionAdvice<String> adviseFoo3() {
                return str -> str += "-advised3";
            }
        });        
        
        System.out.println(injector.getInstance(String.class));
        System.out.println(injector.getInstance(Key.get(String.class, Names.named("foo"))));
    }
}
