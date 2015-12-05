package com.netflix.governator;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.governator.ProvisionMetrics.Element;
import com.netflix.governator.ProvisionMetrics.Visitor;

public class LoggingProvisionMetricsVisitor implements Visitor {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingProvisionMetricsVisitor.class);

    int level = 1;
    int elementCount = 0;
    
    @Override
    public void visit(Element entry) {
        elementCount++;
        LOG.info(String.format("%" + (level * 3 - 2) + "s%s%s : %d ms (%d ms)", 
                "",
                entry.getKey().getTypeLiteral().toString(), 
                entry.getKey().getAnnotation() == null ? "" : " [" + entry.getKey().getAnnotation() + "]",
                entry.getTotalDuration(TimeUnit.MILLISECONDS),
                entry.getDuration(TimeUnit.MILLISECONDS)
                ));
        level++;
        entry.accept(this);
        level--;
    }
    
    int getElementCount() {
        return elementCount;
    }
}
