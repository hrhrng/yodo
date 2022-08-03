package xyz.hrhrng.yodo;

import java.util.HashMap;
import java.util.Map;

public class LoadingStrategyExtensionLoader extends ExtensionLoader{

    public LoadingStrategyExtensionLoader(Class type) {
        super(type);
    }

    private Map<String, Class<? extends LoadingStrategy>> loadExtensionClasses() throws InstantiationException, IllegalAccessException {
        Map<String, Class<? extends LoadingStrategy>> extensionClasses = new HashMap<>();
        SPI defaultAnnotation = (SPI) type.getAnnotation(SPI.class);
        Class defaultStrategy = null;
        LoadingStrategy strategy = null;

        strategy = (LoadingStrategy) defaultStrategy.newInstance();

        loadDirectory0(extensionClasses, type.getName(), strategy);
        return (Map<String, Class<? extends LoadingStrategy>>) extensionClasses;
    }

    public Map<String, Class<? extends LoadingStrategy>> getStrategyMap() {
        return (Map<String, Class<? extends LoadingStrategy>>) cachedClasses.get();
    }

}
