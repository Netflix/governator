package com.netflix.governator.guice;

import java.util.List;

import javax.inject.Inject;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.netflix.governator.annotations.Modules;

public class TestModuleDependency {
    public static class EmptyModule extends AbstractModule {
        @Override
        protected void configure() {
        }
    }
    
    
    @Modules(include = { AnnotatedModuleDependency.class })
    public static class AnnotatedModule extends EmptyModule { }
    
    public static class AnnotatedModuleDependency extends EmptyModule { }
    
    public static class InjectedModule extends EmptyModule { 
        @Inject
        InjectedModule(InjectedModuleDependency d) { }
    }
    
    public static class InjectedModuleDependency extends EmptyModule { }
    
    @Modules(include = { ReplacementAnnotatedModuleDependency.class } )
    public static class ReplacementAnnotatedModule extends EmptyModule {}
    
    public static class ReplacementAnnotatedModuleDependency extends EmptyModule {}
    
    public static class ReplacementInjectedModule extends InjectedModule {
        @Inject
        ReplacementInjectedModule(ReplacementInjectedModuleDependency d) { super(d); }
    }
    
    public static class ReplacementInjectedModuleDependency extends InjectedModuleDependency {}
    
    @Modules(include= {AnnotatedModule.class})
    public static class ParentAnnotatedModule extends EmptyModule {}
    
    @Test
    public void shouldImportAnnotatedDependency() throws Exception {
        List<Module> modules = new ModuleListBuilder()
            .include(AnnotatedModule.class)
            .build(Guice.createInjector());
        
        assertEquals(modules, AnnotatedModuleDependency.class, AnnotatedModule.class);
    }

    @Test
    public void shouldImportInjectedDependency() throws Exception{
        List<Module> modules = new ModuleListBuilder()
            .include(InjectedModule.class)
            .build(Guice.createInjector());
        
        assertEquals(modules, InjectedModuleDependency.class, InjectedModule.class);
    }
    
    @Test
    public void shouldExcludeAnnotatedDependency() throws Exception{
        List<Module> modules = new ModuleListBuilder()
            .include(AnnotatedModule.class)
            .exclude(AnnotatedModuleDependency.class)
            .build(Guice.createInjector());
        
        assertEquals(modules, AnnotatedModule.class);
    }
    
    @Test
    public void shouldExcludeAnnotatedModule() throws Exception{
        List<Module> modules = new ModuleListBuilder()
            .include(AnnotatedModule.class)
            .exclude(AnnotatedModule.class)
            .build(Guice.createInjector());
        
        assertEquals(modules);
    }
    
    @Test
    public void shouldExcludeInjectedModule() throws Exception{
        List<Module> modules = new ModuleListBuilder()
            .include(InjectedModule.class)
            .exclude(InjectedModule.class)
            .build(Guice.createInjector());
        
        assertEquals(modules);
    }
    
    @Test
    public void shouldExcludeInjectedModuleDependency2() throws Exception{
        List<Module> modules = new ModuleListBuilder()
            .include(InjectedModule.class)
            .exclude(InjectedModuleDependency.class)
            .build(Guice.createInjector());
        
        assertEquals(modules, InjectedModule.class);
    }
    
    @Test
    public void shouldIncludeMultipleLevels() throws Exception{
        List<Module> modules = new ModuleListBuilder()
            .include(ParentAnnotatedModule.class)
            .build(Guice.createInjector());
    
        assertEquals(modules, AnnotatedModuleDependency.class, AnnotatedModule.class, ParentAnnotatedModule.class);
    }
    
    @Test
    public void shouldInstallAbstractModuleInstance() throws Exception {
        List<Module> modules = new ModuleListBuilder()
            .include(new AbstractModule() {
                @Override
                protected void configure() {
                }
            })
            .build(Guice.createInjector());
        
        Assert.assertEquals(1, modules.size());
    }
    
    @Test
    public void shouldNotExcludeAbstractModuleInstance() throws Exception {
        List<Module> modules = new ModuleListBuilder()
            .include(new AbstractModule() {
                @Override
                protected void configure() {
                }
            })
            .exclude(AbstractModule.class)
            .build(Guice.createInjector());
        
        Assert.assertEquals(1, modules.size());
    }
    
    public void assertEquals(List<Module> actual, Class<? extends Module> ... expected) {
        Assert.assertEquals(
            Lists.newArrayList(expected), 
            ImmutableList.copyOf(Lists.transform(actual, new Function<Module, Class<? extends Module>>() {
                @Override
                public Class<? extends Module> apply(Module module) {
                    return module.getClass();
                }
            })));
    }

}
