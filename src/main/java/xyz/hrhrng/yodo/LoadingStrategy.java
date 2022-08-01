package xyz.hrhrng.yodo;


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

    /**
     * Indicates current {@link LoadingStrategy} supports overriding other lower prioritized instances or not.
     *
     * @return if supports, return <code>true</code>, or <code>false</code>
     * @since 2.7.7
     */
    default boolean overridden() {
        return false;
    }
    // 用此策略来加载自定义的其他 LoadingStrategy
    class InitialLoadingStrategy implements LoadingStrategy{
        @Override
        public String directory() {
            return "META-INF/yodo";
        }

    }
}
