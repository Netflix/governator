package com.netflix.governator.conditional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.netflix.governator.PropertiesPropertySource;

public class ConditionalBinderTest {
    @Rule
    public TestName name = new TestName();
    
    public static interface Foo {
        public String getName();
    }
    
    public static class FooImpl implements Foo {
        private final String name;

        public FooImpl(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
    
    @Singleton
    static class SingletonFoo implements Foo {
        static int injectedCount = 0;
        
        @Inject
        SingletonFoo() {
            injectedCount++;
        }

        @Override
        public String getName() {
            return "singleton";
        }
    }
    
    static class NonSingletonFoo implements Foo {
        static int injectedCount = 0;
        
        @Inject
        NonSingletonFoo() {
            injectedCount++;
        }

        @Override
        public String getName() {
            return "nonsingleton";
        }
    }
    
    @Before
    public void before() {
        SingletonFoo.injectedCount = 0;
        NonSingletonFoo.injectedCount = 0;
    }
    
    public static class ModuleGroup1 extends AbstractModule {
        @Override
        protected void configure() {
            ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class);
            
            binder.whenMatch(new ConditionalOnProperty("group1", "a"))
                .toInstance(new FooImpl("group1_a"));
            binder.whenMatch(new ConditionalOnProperty("group1", "b"))
                .toInstance(new FooImpl("group1_b"));
            binder.whenNoMatch()
                .toInstance(new FooImpl("group1_default"));
        }
    }
    
    public static class ModuleGroup2 extends AbstractModule {
        @Override
        protected void configure() {
            ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class, Names.named("group2"));
            
            binder.whenMatch(new ConditionalOnProperty("group2", "a"))
                .toInstance(new FooImpl("group2_a"));
            binder.whenMatch(new ConditionalOnProperty("group2", "b"))
                .toInstance(new FooImpl("group2_b"));
        }
    }
    
    public static class ModuleNoConditional extends AbstractModule {
        @Override
        protected void configure() {
            bind(Key.get(Foo.class)).toInstance(new FooImpl("unconditional"));
        }
    }
    
    @Test
    public void bindToMatchedOfMultipleConditionals() {
        Injector injector = Guice.createInjector(
            new PropertiesPropertySource()
                .setProperty("group1", "a"),
            new ModuleGroup1());
        
        Foo foo1 = injector.getInstance(Key.get(Foo.class));
        Assert.assertEquals(foo1.getName(), "group1_a");
    }
    
    @Test
    public void simpleWithDefault() {
        Injector injector = Guice.createInjector(
            new PropertiesPropertySource(),
            new ModuleGroup1());
        
        Foo foo1 = injector.getInstance(Key.get(Foo.class));
        Assert.assertEquals(foo1.getName(), "group1_default");
    }
    
    @Test
    public void differentiateBetweenMultipleQualifiers() {
        Injector injector = Guice.createInjector(
            new PropertiesPropertySource()
                .setProperty("group1", "a")
                .setProperty("group2", "b"),
            new ModuleGroup1(), 
            new ModuleGroup2());
        
        Foo foo1 = injector.getInstance(Key.get(Foo.class));
        Assert.assertEquals(foo1.getName(), "group1_a");
        
        Foo foo2 = injector.getInstance(Key.get(Foo.class, Names.named("group2")));
        Assert.assertEquals(foo2.getName(), "group2_b");
    }
    
    @Test
    public void overrideConditionalWithNonConditional() {
        Injector injector = Guice.createInjector(Modules.override(
                new PropertiesPropertySource()
                    .setProperty("group1", "a"),
                new ModuleGroup1()
                )
            .with(
                new ModuleNoConditional()));
        
        Foo foo = injector.getInstance(Key.get(Foo.class));
        Assert.assertEquals(foo.getName(), "unconditional");
    }
    
    @Test
    public void bindToSingleConditionalWithNoDefault() {
        Injector injector = Guice.createInjector(
            new PropertiesPropertySource()
                .setProperty("foo", "value"),
            new AbstractModule() {
                @Override
                protected void configure() {
                    ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class);
                    
                    binder.whenMatch(new ConditionalOnProperty("foo", "value"))
                        .toInstance(new FooImpl("value"));
                }
            });
        
        Foo foo = injector.getInstance(Key.get(Foo.class));
        Assert.assertEquals(foo.getName(), "value");
    }

    @Test
    public void bindToJustDefault() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class);
                
                binder.whenNoMatch()
                    .toInstance(new FooImpl("default"));
            }
        });
        
        Foo foo = injector.getInstance(Key.get(Foo.class));
        Assert.assertEquals(foo.getName(), "default");
    }

    @Test
    public void confirmSingletonConditionalBehavior() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class);
                binder.whenNoMatch().to(SingletonFoo.class);
                
                bind(SingletonFoo.class);
            }            
        });
        
        Assert.assertEquals(0, SingletonFoo.injectedCount);
        Foo foo1 = injector.getInstance(Foo.class);
        Assert.assertEquals(1, SingletonFoo.injectedCount);
        Foo foo2 = injector.getInstance(Foo.class);
        Assert.assertEquals(1, SingletonFoo.injectedCount);
    }
    
    @Test
    public void confirmSingletonNotEagerlyCreated() {
        Assert.assertEquals(0, SingletonFoo.injectedCount);
        Assert.assertEquals(0, NonSingletonFoo.injectedCount);
        
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class);
                binder.whenMatch(new ConditionalOnProperty("foo", "1")).to(SingletonFoo.class);
                binder.whenNoMatch().to(NonSingletonFoo.class);
            }
        });
        
        Assert.assertEquals(0, SingletonFoo.injectedCount);
        Assert.assertEquals(0, NonSingletonFoo.injectedCount);
        Foo foo1 = injector.getInstance(Foo.class);
        Foo foo2 = injector.getInstance(Foo.class);
        Assert.assertEquals(0, SingletonFoo.injectedCount);
        Assert.assertEquals(2, NonSingletonFoo.injectedCount);
        Assert.assertEquals(foo1.getName(), "nonsingleton");
    }
    
    @Test(expected=CreationException.class)
    public void failWithNoMatchedConditionals(){
        try {
            Guice.createInjector(new ModuleGroup1(), new ModuleGroup2());
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
   
    @Test(expected=CreationException.class)
    public void failWithNoBindTo(){
        try {
            Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class);
                    }
                });
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    @Test(expected=CreationException.class)
    public void failWithDuplicateMatchedConditionals(){
        try {
            Guice.createInjector(
                new PropertiesPropertySource()
                    .setProperty("group1", "a"),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class);
                        
                        binder.whenMatch(new ConditionalOnProperty("group1", "a"))
                            .toInstance(new FooImpl("group1_a"));
                        binder.whenMatch(new ConditionalOnProperty("group1", "a"))
                            .toInstance(new FooImpl("group1_b"));
                    }
                });
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
   
    @Test(expected=CreationException.class)
    public void failOnMultipleDefaults() {
        try {
            Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class);
                    
                    binder.whenNoMatch()
                        .toInstance(new FooImpl("default1"));
                    binder.whenNoMatch()
                        .toInstance(new FooImpl("default2"));
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test(expected=CreationException.class)
    public void conflictingConditionalAndNonConditional() {
        try {
            Guice.createInjector(
                new PropertiesPropertySource()
                    .setProperty("group1", "a"),
                new ModuleGroup1(),
                new ModuleNoConditional());
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    @Test(expected=CreationException.class)
    public void productionStageNotSupported() {
        try {
            Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
                @Override
                protected void configure() {
                    ConditionalBinder<Foo> binder = ConditionalBinder.newConditionalBinder(binder(), Foo.class);
                    
                    binder.whenNoMatch().to(SingletonFoo.class);
                    binder.whenMatch(new ConditionalOnProperty("foo", "1")).to(NonSingletonFoo.class);
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }
}
