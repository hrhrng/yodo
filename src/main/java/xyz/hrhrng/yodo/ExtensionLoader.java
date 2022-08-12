package xyz.hrhrng.yodo;


import xyz.hrhrng.yodo.annotation.SPI;
import xyz.hrhrng.yodo.annotation.Strategy;
import xyz.hrhrng.yodo.common.SimpleLoadingCache;
import xyz.hrhrng.yodo.helper.ExtensionLoaderHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;


public class ExtensionLoader<T> {

    private final SimpleLoadingCache<Class<? extends T>, T> EXTENSION_INSTANCES = new SimpleLoadingCache<>(64);
    protected final Class<?> type;

    // 别名和类是一对多的关系
    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    private volatile Class<?> cachedAdaptiveClass = null;

    private String cachedDefaultName;

    // for wrap
    private final ExtensionFactory objectFactory;



    private volatile Throwable createAdaptiveInstanceError;
    private Optional<Object> cachedAdaptiveInstance = Optional.empty();
    // name->class , instance 延时创建
    // cachedClasses 也是延时创建
    protected Optional<Map<String, Class<? extends T>>> cachedClasses = Optional.empty();
    private SimpleLoadingCache<String, T> cachedInstances = new SimpleLoadingCache<>();
    // class -> name 一对多
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();


    public ExtensionLoader(Class<?> type) {
        this.type = type;
        // 利用本身的机制来加载ExtensionFactory类
        // todo ExtensionFactory
//        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoaderHelper.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
        objectFactory = null;
    }

    public static ExtensionLoader<ExtensionFactory> getExtensionLoader(Class<ExtensionFactory> extensionFactoryClass) {
        return null;
    }


    public T getExtension (String name) {
        return getExtension(name, true);
    }
    public T getExtension (String name, boolean wrap) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Extension name == null");
        }
        if ("true".equals(name)) {
            return getDefaultExtension();
        }
        T instance = cachedInstances.get(name, item -> createExtension(name, true));
        return instance;
    }

    private T getDefaultExtension() {
        return null;
    }


    private T createExtension(String name, boolean wrap) {
        // name -> class
        // class -> instance
        // name  -> instance
        // 这里进行扫描
        Class<T> clazz = (Class<T>) getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        T instance = (T) EXTENSION_INSTANCES.get(clazz, item ->{
            try {
                return item.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        injectExtension(instance);

        //todo wrap

        return instance;
    }



    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();

    // todo 异常
    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);


        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }

            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(entry.getValue().toString());
        }
        return new IllegalStateException(buf.toString());
    }

    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        if (!cachedAdaptiveInstance.isPresent()) {
            if (createAdaptiveInstanceError != null) {
                throw new IllegalStateException("Failed to create adaptive instance: " +
                        createAdaptiveInstanceError.toString(),
                        createAdaptiveInstanceError);
            }
            synchronized (cachedAdaptiveInstance) {
                if (!cachedAdaptiveInstance.isPresent()) {
                    try {
                        Object instance =  createAdaptiveExtension();
                        cachedAdaptiveInstance = Optional.ofNullable(instance);
                    } catch (Throwable t) {
                        createAdaptiveInstanceError = t;
                        throw new IllegalStateException("Failed to create adaptive instance: " + t.toString(), t);
                    }
                }
            }
        }
        return (T) cachedAdaptiveInstance.get();
    }

    private Object createAdaptiveExtension() {
        try {
            return injectExtension((T) getAdaptiveExtensionClass().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Can't create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }

    // 向扩展实例中注入依赖，手工编码
    private Object injectExtension(T newInstance) {
        return newInstance;
    }

    private Class<?> getAdaptiveExtensionClass() {
        // 这里获取所有的Extension
        getExtensionClasses();
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    // todo 自适应
    private Class<?> createAdaptiveExtensionClass() {
        return null;
    }

    // todo findClass
    private ClassLoader findClassLoader() {

        Class clazz = type;
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = clazz.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }

        return cl;
    }

    private Map<String, Class<? extends T>> getExtensionClasses() {
        if (!cachedClasses.isPresent()) {
            synchronized (cachedClasses) {
                if (!cachedClasses.isPresent()) {
                    cachedClasses = Optional.of(loadExtensionClasses());
                }
            }
        }
        return cachedClasses.get();
    }

    private Map<String, Class<? extends T>> loadExtensionClasses() {
        cacheDefaultExtensionName();

        Map<String, Class<? extends T>> extensionClasses = new HashMap<>();
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        final Strategy annoStrategy = type.getAnnotation(Strategy.class);
        // 指定 strategy
        if(defaultAnnotation.strategy().length != 0 || annoStrategy != null){
            if (defaultAnnotation.strategy().length != 0) {
                for (String s : defaultAnnotation.strategy()){
                    LoadingStrategy strategy = ExtensionLoaderHelper.loadingStrategyCache.get(defaultAnnotation.strategy());
                    loadDirectory0(extensionClasses, type.getName(), strategy);
                }
            }
            if (annoStrategy != null) {
                loadDirectory(extensionClasses, annoStrategy.directory(), type.getName(),
                        annoStrategy.preferExtensionClassLoader(), annoStrategy.overridden(), annoStrategy.excludedPackages());
            }
        }
        else {
            for (LoadingStrategy strategy : ExtensionLoaderHelper.strategies) {
                loadDirectory0(extensionClasses, type.getName(), strategy);
            }
        }
        return extensionClasses;
    }
    void loadDirectory0(Map<String, Class<? extends T>> extensionClasses, String type, LoadingStrategy strategy) {
        loadDirectory(extensionClasses, strategy.directory(), type, strategy.preferExtensionClassLoader(), strategy.overridden(), strategy.excludedPackages());
    }

    // 根据测量器来
    // todo dubbo 没有用到的字段能否在注解中提供，策略可不可以在注解中指定？
    // 获取 SPI 定义的 默认扩展名
    // TODO 默认的扩展名可不可以在 meta 文件中 定义

    void loadDirectory(Map<String, Class<? extends T>> extensionClasses, String dir, String type,
                       boolean extensionLoaderClassLoaderFirst, boolean overridden, String... excludedPackages) {
        String fileName = dir + type;
        try {
            Enumeration<java.net.URL> urls = null;
            ClassLoader classLoader = findClassLoader();

            // try to load from ExtensionLoader's ClassLoader first
            if (extensionLoaderClassLoaderFirst) {
                ClassLoader extensionLoaderClassLoader = ExtensionLoader.class.getClassLoader();
                if (ClassLoader.getSystemClassLoader() != extensionLoaderClassLoader) {
                    urls = extensionLoaderClassLoader.getResources(fileName);
                }
            }

            if (urls == null || !urls.hasMoreElements()) {
                if (classLoader != null) {
                    urls = classLoader.getResources(fileName);
                } else {
                    urls = ClassLoader.getSystemResources(fileName);
                }
            }

            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceURL = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceURL, overridden, excludedPackages);
                }
            }
        } catch (Throwable t) {
//            logger.error("Exception occurred when loading extension class (interface: " +
//                    type + ", description file: " + fileName + ").", t);
        }
    }

    private void loadResource(Map<String, Class<? extends T>> extensionClasses, ClassLoader classLoader,
                              URL resourceURL, boolean overridden, String... excludedPackages) {
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final int ci = line.indexOf('#');
                    if (ci >= 0) {
                        line = line.substring(0, ci);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        try {
                            String name = null;
                            int i = line.indexOf('=');
                            if (i > 0) {
                                name = line.substring(0, i).trim();
                                line = line.substring(i + 1).trim();
                            }
                            if (line.length() > 0 && !isExcluded(line, excludedPackages)) {
                                loadClass(extensionClasses, resourceURL, Class.forName(line, true, classLoader), name, overridden);
                            }
                        } catch (Throwable t) {
                            IllegalStateException e = new IllegalStateException("Failed to load extension class (interface: " + type + ", class line: " + line + ") in " + resourceURL + ", cause: " + t.getMessage(), t);
                            exceptions.put(line, e);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // todo log
//            logger.error("Exception occurred when loading extension class (interface: " +
//                    type + ", class file: " + resourceURL + ") in " + resourceURL, t);

        }
    }

    private void loadClass(Map<String, Class<? extends T>> extensionClasses, URL resourceURL, Class<?> clazz, String name,
                           boolean overridden) throws NoSuchMethodException {

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading extension class (interface: " +
                    type + ", class line: " + clazz.getName() + "), class "
                    + clazz.getName() + " is not subtype of interface.");
        }

        clazz.getConstructor();

        if (isEmpty(name)) {
//            name = findAnnotationName(clazz);
            throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
        }

        String[] names = NAME_SEPARATOR.split(name);
        if (names.length != 0 && names != null ) {
//            cacheActivateClass(clazz, names[0]);
            for (String n : names) {
                cacheName(clazz, n);
                saveInExtensionClass(extensionClasses, clazz, n, overridden);
            }
        }
    }

    private void saveInExtensionClass(Map<String, Class<? extends T>> extensionClasses, Class<?> clazz, String name, boolean overridden) {
        Class<?> c = extensionClasses.get(name);
        if (c == null || overridden) {
            extensionClasses.put(name, (Class<? extends T>) clazz);
        } else if (c != clazz) {
            String duplicateMsg = "Duplicate extension " + type.getName() + " name " + name + " on " + c.getName() + " and " + clazz.getName();
//            logger.error(duplicateMsg);
            throw new IllegalStateException(duplicateMsg);
        }
    }

    private void cacheName(Class<?> clazz, String name) {
        if (!cachedNames.containsKey(clazz)) {
            cachedNames.put(clazz, name);
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private boolean isExcluded(String className, String... excludedPackages) {
        if (excludedPackages != null) {
            for (String excludePackage : excludedPackages) {
                if (className.startsWith(excludePackage + ".")) {
                    return true;
                }
            }
        }
        return false;
    }
    private void cacheDefaultExtensionName() {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        // TODO pin
        if (defaultAnnotation == null) {
            return;
        }

        String value = defaultAnnotation.value();
        if ((value = value.trim()).length() > 0) {
            String[] names = NAME_SEPARATOR.split(value);
            if (names.length > 1) {
                throw new IllegalStateException("More than 1 default extension name on extension " + type.getName()
                        + ": " + Arrays.toString(names));
            }
            if (names.length == 1) {
                cachedDefaultName = names[0];
            }
        }
    }

    public String[] getSupportedExtensions() {
        return new String[0];
    }
}
