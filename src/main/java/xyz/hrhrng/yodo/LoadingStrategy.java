package xyz.hrhrng.yodo;


import xyz.hrhrng.yodo.annotation.SPI;
import xyz.hrhrng.yodo.common.Prioritized;

// todo 是否可以简化策略的内容
@SPI(strategy = "xyz.hrhrng.yodo.Prioritized.InitialLoadingStrategy")
public interface LoadingStrategy extends Prioritized {

    String directory();

    default boolean preferExtensionClassLoader() {
        return false;
    }

    default String[] excludedPackages() {
        return null;
    }

    default boolean overridden() {
        return false;
    }

    /**
     * defaultLoadingStrategy 的作用：
     *  1.作为默认的 LoadingStrategy
     *  2.用户的自定义的 LoadingStrategy 必须定义在此
     */
    class defaultLoadingStrategy implements LoadingStrategy{
        @Override
        public String directory() {
            return "META-INF/yodo";
        }

    }
}
