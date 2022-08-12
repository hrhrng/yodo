package xyz.hrhrng.yodo;

import xyz.hrhrng.yodo.annotation.SPI;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LoadingStrategyExtensionLoader extends ExtensionLoader<LoadingStrategy>{

    private LoadingStrategy defaultLoadingStrategy;

    public LoadingStrategyExtensionLoader(Class type) {
        super(type);
    }

    private Map<String, Class<? extends LoadingStrategy>> loadExtensionClasses() throws InstantiationException, IllegalAccessException {
        Map<String, Class<? extends LoadingStrategy>> extensionClasses = new HashMap<>();

        Class defaultStrategyClass = LoadingStrategy.defaultLoadingStrategy.class;

        LoadingStrategy strategy = (LoadingStrategy) defaultStrategyClass.newInstance();

        extensionClasses.put("default", defaultStrategyClass);

        defaultLoadingStrategy = strategy;

        loadDirectory0(extensionClasses, type.getName(), strategy);

        return extensionClasses;
    }

    public Map<String, Class<? extends LoadingStrategy>> getStrategyMap() {
        try {
            cachedClasses = Optional.of(loadExtensionClasses());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return  cachedClasses.get();
    }

    public LoadingStrategy getDefault() {
        return defaultLoadingStrategy;
    }
}
