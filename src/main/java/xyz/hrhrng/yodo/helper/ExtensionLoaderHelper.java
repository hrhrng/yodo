package xyz.hrhrng.yodo.helper;

import xyz.hrhrng.yodo.ExtensionLoader;
import xyz.hrhrng.yodo.LoadingStrategy;
import xyz.hrhrng.yodo.LoadingStrategyExtensionLoader;
import xyz.hrhrng.yodo.annotation.SPI;
import xyz.hrhrng.yodo.common.SimpleLoadingCache;

import java.util.HashMap;
import java.util.Map;

public class ExtensionLoaderHelper {

    // cache the ExtensionLoader
    private static final SimpleLoadingCache<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new SimpleLoadingCache<>(64);

    // 策略作为公共的模块

    // volatile 防止指令重排
    // 为了指定某个策略
    public static volatile Map<String, LoadingStrategy> loadingStrategyCache = loadLoadingStrategies();

    public static volatile LoadingStrategy[] strategies =
            loadingStrategyCache.entrySet().stream().map(i->i.getValue()).toArray(LoadingStrategy[]::new);

    // 工厂方法
    // static 要使用 传入的参数 的泛型，需要在 返回值前 声明泛型
    // 和dubbo不同的是，type可以为Class

    // JDK SPI 机制获取策略器
    private static Map<String, LoadingStrategy> loadLoadingStrategies() {

        LoadingStrategyExtensionLoader extensionLoader =
                (LoadingStrategyExtensionLoader)EXTENSION_LOADERS.get(LoadingStrategy.class,LoadingStrategyExtensionLoader::new);

        // META-INFO/yodo/xyz.hrhrng.yodo.LoadingStrategy
        Map<String, Class<? extends LoadingStrategy>> loadingStrategyCache1 =  extensionLoader.getStrategyMap();

        Map<String, LoadingStrategy> loadingStrategyCache = new HashMap<>();

        loadingStrategyCache1.entrySet().stream().map(i->{
            try {
                loadingStrategyCache.put(i.getKey(), extensionLoader.getExtension(i.getKey()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        loadingStrategyCache.put("defualt", extensionLoader.getDefault());

        return loadingStrategyCache;

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
