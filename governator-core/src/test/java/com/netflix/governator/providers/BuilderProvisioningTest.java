package com.netflix.governator.providers;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.netflix.governator.annotations.binding.BuilderAdvice;
import com.netflix.governator.annotations.binding.ProvidesBuilder;
import com.netflix.governator.providers.BuilderProvisioning;

public class BuilderProvisioningTest {
    private final static TypeLiteral<ListBuilder> LIST_BUILDER_TYPE = new TypeLiteral<ListBuilder>() {};
    
    @Test
    public void testMultipleModulesBuilder() {
        Injector injector = Guice.createInjector(new ProvidesBuilderModule(), new ConsumesBuilderModule());
        Provider<ListBuilder> listBuilder = injector.getProvider(ProvidesBuilderModule.BUILDER_KEY);
        
        List<String> list = listBuilder.get().build();
        Assert.assertTrue(list.contains("consumer1"));
        Assert.assertTrue(list.contains("four"));
        Assert.assertTrue(list.contains("five"));
        Assert.assertTrue(list.contains("consumer2"));
        Assert.assertTrue(list.contains("nine"));
    }

    @Test
    public void testProgrammaticBuilder() {
        Injector injector = Guice.createInjector(new ProgrammaticBuilderTestModule());

        // check that programmatically-bound builder is modified by consumers
        List<String> pbList = injector.getInstance(ProgrammaticBuilderTestModule.PROGRAMMATIC_BUILDER_KEY).build();
        Assert.assertTrue(pbList.contains("pbAdvice1"));
        Assert.assertTrue(pbList.contains("sixes"));
        Assert.assertTrue(pbList.contains("sevens"));
        Assert.assertTrue(pbList.contains("pbAdvice2"));
        Assert.assertTrue(pbList.contains("nines"));

        // check that annotated builder is modified by consumers
        List<String> list = injector.getInstance(ProgrammaticBuilderTestModule.ANNOTATED_BUILDER_KEY).build();
        Assert.assertTrue(list.contains("consumer1"));
        Assert.assertTrue(list.contains("four"));
        Assert.assertTrue(list.contains("five"));
        Assert.assertTrue(list.contains("consumer2"));
        Assert.assertTrue(list.contains("nine"));

    }

    @Test
    public void testFunctionalBuilder() {
        Key<ListBuilder> functionalBuilderKey = Key.get(LIST_BUILDER_TYPE, Names.named("functionalBuilder"));

        Injector injector = Guice.createInjector(new ConsumesBuilderModule() {
            public void configure() {
                super.configure();
                BuilderProvisioning.bindBuilder(binder(), "listBuilder", functionalBuilderKey).toProvider(() -> {
                    return new ListBuilder("1", "2", "3");
                });
            }
        });

        // check that programmatically-bound functional builder is modified by consumers
        ListBuilder builder = injector.getInstance(functionalBuilderKey);
        List<String> list = builder.build();
        Assert.assertTrue(list.contains("consumer1"));
        Assert.assertTrue(list.contains("four"));
        Assert.assertTrue(list.contains("five"));
        Assert.assertTrue(list.contains("consumer2"));
        Assert.assertTrue(list.contains("nine"));
    }
    
    @Test
    public void testInjectedBuilder() {
        Key<ListBuilder> listBuilderKey = Key.get(LIST_BUILDER_TYPE, Names.named(InjectedBean.BUILDER_NAME));

        Injector injector = Guice.createInjector(new ConsumesBuilderModule() {
            public void configure() {
                super.configure();
                bind(InjectedBean.class);
                BuilderProvisioning.bindBuilder(binder(), "listBuilder", listBuilderKey).toProvider(() -> {
                    return new ListBuilder("1", "2", "3");
                });
            }
        });

        // check that programmatically-bound functional builder is modified by consumers
        InjectedBean injectedBean = injector.getInstance(InjectedBean.class);
        List<String> list = injectedBean.getMyList();
        Assert.assertTrue(list.contains("consumer1"));
        Assert.assertTrue(list.contains("four"));
        Assert.assertTrue(list.contains("five"));
        Assert.assertTrue(list.contains("consumer2"));
        Assert.assertTrue(list.contains("nine"));
    }    
    
    @Test
    public void testProgrammaticBuilderAdvice() {
        Key<Consumer<ListBuilder>> consumerKey = Key.get(new TypeLiteral<Consumer<ListBuilder>>(){});
        
        Injector injector = Guice.createInjector(new ProvidesBuilderModule(), new AbstractModule() {
            
            @Override
            protected void configure() {
                BuilderProvisioning.bindAdvice(binder(), ProvidesBuilderModule.BUILDER_NAME, consumerKey).toInstance(builder->builder.add("programmaticConsumer1"));
                BuilderProvisioning.bindAdvice(binder(), ProvidesBuilderModule.BUILDER_NAME, consumerKey).toInstance(builder->builder.add("programmaticConsumer2"));
                
            }
        });

        List<String> list = injector.getInstance(ProvidesBuilderModule.BUILDER_KEY).build();
        Assert.assertTrue(list.contains("programmaticConsumer1"));
        Assert.assertTrue(list.contains("programmaticConsumer2"));
    }

    

    private static final class ProgrammaticBuilderTestModule extends AbstractModule {
        static final Key<ListBuilder> PROGRAMMATIC_BUILDER_KEY = Key.get(LIST_BUILDER_TYPE, Names.named("programmaticBuilder"));
        static final Key<ListBuilder> ANNOTATED_BUILDER_KEY = Key.get(LIST_BUILDER_TYPE, Names.named("annotatedBuilder"));

        
        public void configure() {
            BuilderProvisioning.bindBuilder(binder(), "programmaticBuilder", PROGRAMMATIC_BUILDER_KEY).toProvider(
                    new Provider<ListBuilder>() {
                        @Override
                        public ListBuilder get() {
                            return new ListBuilder("one", "two", "three");
                        }
                    }                        
            );
        }

        @Named("annotatedBuilder")
        @ProvidesBuilder
        public ListBuilder simpleProvider() {
            return new ListBuilder("1", "2", "3");
        }

        @BuilderAdvice       
        public Consumer<ListBuilder> nineAdvice() {
            return list -> {
                list.add("consumer2");
                list.add("nine");
            };
        }

        @BuilderAdvice        
        public Consumer<ListBuilder> fourAndFiveAdvice() {
            return list -> {
                list.add("consumer1");
                list.add("four");
                list.add("five");
            };
        }

        @BuilderAdvice("programmaticBuilder")     
        public Consumer<ListBuilder> pbAdvice1() {
            return list -> {
                list.add("pbAdvice1");
                list.add("nines");
            };
        }

        @BuilderAdvice("programmaticBuilder")        
        public Consumer<ListBuilder> pbAdvice2() {
            return list -> {
                list.add("pbAdvice2");
                list.add("sixes");
                list.add("sevens");
            };
        }
    }

    /**
     * demonstrates usage of the builder pattern by consuming a mutable list as 'builder' and changing its contents
     *
     */
    private static class ConsumesBuilderModule extends AbstractModule {
        @Override
        protected void configure() {
        }

        @BuilderAdvice(ProvidesBuilderModule.BUILDER_NAME)        
        public Consumer<ListBuilder> nineAdvice() {
            return list -> {
                list.add("consumer2");
                list.add("nine");
            };
        }

        @BuilderAdvice(ProvidesBuilderModule.BUILDER_NAME)        
        public Consumer<ListBuilder> fourAndFiveAdvice() {
            return list -> {
                list.add("consumer1");
                list.add("four");
                list.add("five");
            };
        }
    }

    /**
     * demonstrates the builder pattern by providing a builder named 'provider1' creates a mutable list as the 'Builder'
     * object
     *
     */
    private static final class ProvidesBuilderModule extends AbstractModule {
        final static String BUILDER_NAME="listBuilder";
        final static Key<ListBuilder> BUILDER_KEY = Key.get(LIST_BUILDER_TYPE);
        
        @Override
        protected void configure() {
            BuilderProvisioning.bind(binder());
        }

        @ProvidesBuilder(BUILDER_NAME)
        public ListBuilder simpleProvider() {
            return new ListBuilder("one", "two", "three");
        }

    }
    
    private static class InjectedBean {        
        final static String BUILDER_NAME="listBuilder";
        private List<String> myList;
        @Inject
        public InjectedBean(@Named(BUILDER_NAME) ListBuilder listBuilder) {
            this.myList = listBuilder.build();
        }
        
        public List<String> getMyList() {
            return myList;
        }
    }

}
