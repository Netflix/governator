package custom_autobind;

import com.google.inject.BindingAnnotation;
import com.netflix.governator.annotations.AutoBind;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
@BindingAnnotation
@AutoBind
public @interface ExampleAutoBind
{
    String  propertyName();

    String  defaultValue();
}
