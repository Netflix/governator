package com.netflix.governator.conditional;

public abstract class AbstractConditional extends Conditional {
    @Override
    public Conditional and(Conditional conditional) {
        return new AndConditional(this, conditional);
    }

    @Override
    public Conditional or(Conditional conditional) {
        return new OrConditional(this, conditional);
    }

}
