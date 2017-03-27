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
import java.util.List;
import java.util.function.UnaryOperator;

import javax.inject.Named;

public class AdvisesBinderTest {
    static class AdviseList implements UnaryOperator<List<String>> {
        @Override
        public List<String> apply(List<String> t) {
            t.add("a");
            return t;
        }
    }
    
    @Test
    public void adviseWithAdvice() {
        TypeLiteral<List<String>> LIST_TYPE_LITERAL =  new TypeLiteral<List<String>>() {};
        
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(AdvisableAnnotatedMethodScanner.asModule());
                
                AdvisesBinder.bind(binder(), LIST_TYPE_LITERAL).toInstance(new ArrayList<>());
                AdvisesBinder.bindAdvice(binder(), LIST_TYPE_LITERAL, 0).to(AdviseList.class);
            }
        });
        
        List<String> list = injector.getInstance(Key.get(LIST_TYPE_LITERAL));
        Assert.assertEquals(Arrays.asList("a"), list);
    }

    @Test
    public void provisionWithoutAdviseDoesntBlowUp() {
        TypeLiteral<List<String>> LIST_TYPE_LITERAL =  new TypeLiteral<List<String>>() {};
        
        Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(AdvisableAnnotatedMethodScanner.asModule());
                
                AdvisesBinder.bindAdvice(binder(), LIST_TYPE_LITERAL, 0).to(AdviseList.class);
            }
        });
    }
    
    @Test
    public void adviseWithoutQualifier() {
        TypeLiteral<List<String>> LIST_TYPE_LITERAL =  new TypeLiteral<List<String>>() {};
        
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(AdvisableAnnotatedMethodScanner.asModule());
                
                AdvisesBinder.bind(binder(), LIST_TYPE_LITERAL).toInstance(new ArrayList<>());
                AdvisesBinder.bindAdvice(binder(), LIST_TYPE_LITERAL, 0).to(AdviseList.class);
            }
            
            @Advises
            UnaryOperator<List<String>> advise() {
                return list -> {
                    list.add("b");
                    return list;
                };
            }
            
        });
        
        List<String> list = injector.getInstance(Key.get(LIST_TYPE_LITERAL));
        Assert.assertEquals(Arrays.asList("a", "b"), list);
    }
    
    @Test
    public void adviseWithQualifier() {
        TypeLiteral<List<String>> LIST_TYPE_LITERAL =  new TypeLiteral<List<String>>() {};
        
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(AdvisableAnnotatedMethodScanner.asModule());
                
                AdvisesBinder.bind(binder(), LIST_TYPE_LITERAL, Names.named("test")).toInstance(new ArrayList<>());
                AdvisesBinder.bindAdvice(binder(), LIST_TYPE_LITERAL, Names.named("test"), 0).to(AdviseList.class);
            }
            
            @Advises
            @Named("test")
            UnaryOperator<List<String>> advise() {
                return list -> {
                    list.add("b");
                    return list;
                };
            }
            
        });
        
        List<String> list = injector.getInstance(Key.get(LIST_TYPE_LITERAL, Names.named("test")));
        Assert.assertEquals(Arrays.asList("a", "b"), list);
    }
    
    @Test
    public void adviseNoBleedingBetweenQualifiers() {
        TypeLiteral<List<String>> LIST_TYPE_LITERAL =  new TypeLiteral<List<String>>() {};
        
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(AdvisableAnnotatedMethodScanner.asModule());
                
                AdvisesBinder.bind(binder(), LIST_TYPE_LITERAL, Names.named("test")).toInstance(new ArrayList<>());
                AdvisesBinder.bindAdvice(binder(), LIST_TYPE_LITERAL, Names.named("test"), 0).to(AdviseList.class);
                
                AdvisesBinder.bind(binder(), LIST_TYPE_LITERAL).toInstance(new ArrayList<>());
                AdvisesBinder.bindAdvice(binder(), LIST_TYPE_LITERAL, 0).to(AdviseList.class);
            }
            
            @Advises
            @Named("test")
            UnaryOperator<List<String>> adviseQualified() {
                return list -> {
                    list.add("qualified");
                    return list;
                };
            }
            
            @Advises
            UnaryOperator<List<String>> adviseNotQualified() {
                return list -> {
                    list.add("not qualified");
                    return list;
                };
            }
            
        });
        
        List<String> list;
        list = injector.getInstance(Key.get(LIST_TYPE_LITERAL, Names.named("test")));
        Assert.assertEquals(Arrays.asList("a", "qualified"), list);
        
        list = injector.getInstance(Key.get(LIST_TYPE_LITERAL));
        Assert.assertEquals(Arrays.asList("a", "not qualified"), list);
    }
}
