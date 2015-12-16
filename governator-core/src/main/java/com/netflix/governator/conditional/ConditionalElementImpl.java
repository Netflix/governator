package com.netflix.governator.conditional;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Key;
import com.google.inject.internal.Annotations;

class ConditionalElementImpl implements ConditionalElement {
    private static final AtomicInteger nextUniqueId = new AtomicInteger(1);

    private final int uniqueId;
    private final String keyName;
    private final Conditional condition;

    ConditionalElementImpl(String keyName, Conditional condition) {
        this(keyName, condition, nextUniqueId.incrementAndGet());
    }

    ConditionalElementImpl(String keyName, Conditional condition, int uniqueId) {
        this.uniqueId = uniqueId;
        this.keyName = keyName;
        this.condition = condition;
    }

    @Override
    public String keyName() {
        return keyName;
    }

    @Override
    public int uniqueId() {
        return uniqueId;
    }

    public Conditional getCondition() {
        return condition;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return ConditionalElement.class;
    }

    @Override
    public String toString() {
        return "@" + ConditionalElement.class.getName() + "(keyName=" + keyName
                + ",uniqueId=" + uniqueId + ",condition=" + condition + ")";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConditionalElement
                && ((ConditionalElement) o).keyName().equals(keyName())
                && ((ConditionalElement) o).uniqueId() == uniqueId();
    }

    @Override
    public int hashCode() {
        return ((127 * "keyName".hashCode()) ^ keyName.hashCode())
             + ((127 * "uniqueId".hashCode()) ^ uniqueId);
    }

    /**
     * Returns the name the binding should use. This is based on the annotation.
     * If the annotation has an instance and is not a marker annotation, we ask
     * the annotation for its toString. If it was a marker annotation or just an
     * annotation type, we use the annotation's name. Otherwise, the name is the
     * empty string.
     */
    static String nameOf(Key<?> key) {
        Annotation annotation = key.getAnnotation();
        Class<? extends Annotation> annotationType = key.getAnnotationType();
        if (annotation != null && !Annotations.isMarker(annotationType)) {
            return key.getAnnotation().toString();
        } 
        else if (key.getAnnotationType() != null) {
            return "@" + key.getAnnotationType().getName();
        } 
        else {
            return "";
        }
    }
}