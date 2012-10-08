package com.netflix.governator.autobind;

import com.google.inject.BindingAnnotation;
import com.netflix.governator.annotations.AutoBind;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@BindingAnnotation
@AutoBind
public @interface CustomAutoBind
{
    String      str();

    int         value();
}
