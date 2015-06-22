package com.netflix.governator.guice.transformer;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.StaticInjectionRequest;
import com.netflix.governator.guice.ModuleTransformer;

public class StripStaticInjections implements ModuleTransformer {
    @Override
    public Collection<Module> call(Collection<Module> modules) {
        final List<Element> noStatics = Lists.newArrayList();
        for(Element element : Elements.getElements(modules)) {
            element.acceptVisitor(new DefaultElementVisitor<Void>() {
                @Override 
                public Void visit(StaticInjectionRequest request) {
                    // override to not call visitOther
                    return null;
                }

                @Override 
                public Void visitOther(Element element) {
                    noStatics.add(element);
                    return null;
                }
            });
        }
        return ImmutableList.<Module>of(Elements.getModule(noStatics));
    }
}
