package xyz.hrhrng.yodo;


import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {
    // 设置默认的 实现 或 子类
    String value() default "";
    // 自定义 LoadingStrategy， 用户自行实现，并在 META-INFO/services/xyz.hrhrng.yodo.LoadingStrategy 中声明
    String strategy() default "";
}
