package xyz.hrhrng.yodo;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Strategy {
    String directory();
    String[] excludedPackages();
}
