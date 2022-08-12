package xyz.hrhrng.yodo.annotation;

import xyz.hrhrng.yodo.LoadingStrategy;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Strategy {
    String directory();
    String[] excludedPackages() default {};

    boolean preferExtensionClassLoader() default false;

    boolean overridden() default false;

}
