package xyz.hrhrng.yodo.helper;

import xyz.hrhrng.yodo.ExtensionLoader;
import xyz.hrhrng.yodo.LoadingStrategy;
import xyz.hrhrng.yodo.LoadingStrategyExtensionLoader;
import xyz.hrhrng.yodo.annotation.SPI;
import xyz.hrhrng.yodo.common.SimpleLoadingCache;
import java.util.Map;

public class ExtensionLoaderHelper {

    // cache the ExtensionLoader
    private static final SimpleLoadingCache<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new SimpleLoadingCache<>(64);

    // 策略作为公共的模块
    // TODO why volatile
    // volatile 防止指令重排
    public static volatile LoadingStrategy[] strategies = loadLoadingStrategies();
    // 为了指定某个策略
    public static volatile Map<String, LoadingStrategy> loadingStrategyCache;

    // 工厂方法
    // static 要使用 传入的参数 的泛型，需要在 返回值前 声明泛型
    // 和dubbo不同的是，type可以为Class

    // JDK SPI 机制获取策略器
    private static LoadingStrategy[] loadLoadingStrategies() {

        LoadingStrategy initial = new LoadingStrategy.defaultLoadingStrategy();

        LoadingStrategyExtensionLoader extensionLoader =
                new LoadingStrategyExtensionLoader(LoadingStrategy.class);
        // META-INFO/yodo/xyz.hrhrng.yodo.LoadingStrategy
        Map<String, Class<?>> loadingStrategyCache1 = (Map<String, Class<?>>) extensionLoader.getStrategyMap();
        loadingStrategyCache1.entrySet().stream().map(i->{
            try {
                loadingStrategyCache.put(i.getKey(), (LoadingStrategy) i.getValue().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        return loadingStrategyCache.entrySet().stream().map(i -> i.getValue()).toArray(LoadingStrategy[]::new);
    }
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {

        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }

        if (!type.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("Extension type (" + type +
                    ") is not an extension, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        }

        return (ExtensionLoader<T>) EXTENSION_LOADERS.get(type, ExtensionLoader<T>::new);
    }
}
