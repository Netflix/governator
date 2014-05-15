package com.netflix.governator.guice.filters;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Module;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.StaticInjectionRequest;
import com.netflix.governator.guice.ModuleFilter;

public class WarnAboutStaticInjections implements ModuleFilter {
    private static Logger LOG = LoggerFactory.getLogger(WarnAboutStaticInjections.class);
    
    @Override
    public Collection<Module> call(Collection<Module> modules) {
        for(Element element : Elements.getElements(modules)) {
            element.acceptVisitor(new DefaultElementVisitor<Void>() {
                @Override 
                public Void visit(StaticInjectionRequest request) {
                    LOG.info("You shouldn't be using static injection at: " + request.getSource());
                    return null;
                }
            });
          }
        return modules;
    }

}
