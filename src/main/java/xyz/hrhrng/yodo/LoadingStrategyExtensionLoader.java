package xyz.hrhrng.yodo;

import xyz.hrhrng.yodo.annotation.SPI;

import java.util.HashMap;
import java.util.Map;

public class LoadingStrategyExtensionLoader extends ExtensionLoader{

    public LoadingStrategyExtensionLoader(Class type) {
        super(type);
    }

    private Map<String, Class<? extends LoadingStrategy>> loadExtensionClasses() throws InstantiationException, IllegalAccessException {
        Map<String, Class<? extends LoadingStrategy>> extensionClasses = new HashMap<>();

        Class defaultStrategy = LoadingStrategy.defaultLoadingStrategy.class;

        LoadingStrategy strategy = (LoadingStrategy) defaultStrategy.newInstance();

        loadDirectory0(extensionClasses, type.getName(), strategy);
        return (Map<String, Class<? extends LoadingStrategy>>) extensionClasses;
    }

    public Map<String, Class<? extends LoadingStrategy>> getStrategyMap() {
        try {
            loadExtensionClasses();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return (Map<String, Class<? extends LoadingStrategy>>) cachedClasses.get();
    }

}
