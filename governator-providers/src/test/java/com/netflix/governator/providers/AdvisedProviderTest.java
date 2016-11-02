package com.netflix.governator.providers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.UnaryOperator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

public class AdvisedProviderTest {
    public static class Bar {
        final List<String> list;

        @Inject
        Bar(@TestQualifier(name="bar") List<String> list) {
            this.list = list;
        }
    }
    
    private static final TypeLiteral<List<String>> LIST_TYPE_LITERAL = new TypeLiteral<List<String>>() {};

    @Test
    public void testCombinationOfStringClassAndNoQualifiers() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(AdvisableAnnotatedMethodScanner.asModule());
            }
            
            @ProvidesWithAdvice
            @Singleton
            List<String> getListWithNoQualifiers() {
                return new ArrayList<>(Arrays.asList(""));
            }
            
            @ProvidesWithAdvice
            @Singleton
            String getStringBuilder() {
                return "0";
            }
            
            @ProvidesWithAdvice
            @Singleton
            @Named("foo")
            List<String> getListWithStringQualifier() {
                return new ArrayList<>(Arrays.asList("foo"));
            }
            
            @ProvidesWithAdvice
            @Singleton
            @TestQualifier(name="bar")
            List<String> getListWithTestQualifier() {
                return new ArrayList<>(Arrays.asList("bar"));
            }
            
            @Advises(order=1)
            UnaryOperator<List<String>> adviseNoQualifier1() {
                return list -> { 
                    list.add("1"); 
                    return list;
                };
            }
            
            @Advises(order=2)
            UnaryOperator<List<String>> adviseNoQualifier2() {
                return list -> { 
                    list.add("2"); 
                    return list;
                };
            }

            @Advises(order=1)
            UnaryOperator<String> adviseNoQualifierStringBuilder1() {
                return str -> str + "1";
            }
            
            @Advises(order=2)
            UnaryOperator<String> adviseNoQualifierStringBuilder2() {
                return str -> str + "2";
            }

            @Advises(order=1)
            @Named("foo")
            UnaryOperator<List<String>> adviseNamedQualifier1() {
                return list -> { 
                    list.add("foo1"); 
                    return list;
                };
            }
            
            @Advises(order=2)
            @Named("foo")
            UnaryOperator<List<String>> adviseNamedQualifier2() {
                return list -> { 
                    list.add("foo2"); 
                    return list;
                };
            }
            
            @Advises(order=1)
            @TestQualifier(name="bar")
            UnaryOperator<List<String>> adviseTestQualifier1() {
                return list -> { 
                    list.add("bar1"); 
                    return list;
                };
            }
            
            @Advises(order=2)
            @TestQualifier(name="bar")
            UnaryOperator<List<String>> adviseTestQualifier2() {
                return list -> { 
                    list.add("bar2"); 
                    return list;
                };
            }
        });        
        
        String noQualifierString = injector.getInstance(String.class);
        List<String> noQualifier = injector.getInstance(Key.get(LIST_TYPE_LITERAL));
        List<String> nameQualifier = injector.getInstance(Key.get(LIST_TYPE_LITERAL, Names.named("foo")));
        Bar bar = injector.getInstance(Bar.class);
        
        Assert.assertEquals("012",  noQualifierString);
        Assert.assertEquals(Arrays.asList("", "1", "2"),  noQualifier);
        Assert.assertEquals(Arrays.asList("foo", "foo1", "foo2"),  nameQualifier);
        Assert.assertEquals(Arrays.asList("bar", "bar1", "bar2"),  bar.list);
    }

    @Test
    public void testDuplicateOrder() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(AdvisableAnnotatedMethodScanner.asModule());
            }
            
            @ProvidesWithAdvice
            @Singleton
            List<String> getListWithNoQualifiers() {
                return new ArrayList<>(Arrays.asList(""));
            }
            
            @Advises(order=1)
            UnaryOperator<List<String>> adviseNoQualifier1() {
                return list -> { 
                    list.add("1"); 
                    return list;
                };
            }
            
            @Advises(order=1)
            UnaryOperator<List<String>> adviseNoQualifier2() {
                return list -> { 
                    list.add("2"); 
                    return list;
                };
            }
        });        
        
        
        List<String> noQualifier = injector.getInstance(Key.get(LIST_TYPE_LITERAL));
        
        Assert.assertEquals(new HashSet<>(Arrays.asList("", "1", "2")),  new HashSet<>(noQualifier));
    }
}
