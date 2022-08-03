package xyz.hrhrng.yodo;


import xyz.hrhrng.yodo.common.SimpleLoadingCache;
import xyz.hrhrng.yodo.helper.ExtensionLoaderHelper;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

public class ExtensionLoader<T> {

    private final SimpleLoadingCache<Class<? extends T>, T> EXTENSION_INSTANCES = new SimpleLoadingCache<>(64);
    protected final Class<?> type;

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    private volatile Class<?> cachedAdaptiveClass = null;

    private String cachedDefaultName;

    // for wrap
    private final ExtensionFactory objectFactory;



    private volatile Throwable createAdaptiveInstanceError;
    // 使用 JDK 自带的 Optional 代替 dubbo 的 Holder。
    private Optional<Object> cachedAdaptiveInstance = Optional.empty();

    protected Optional<Map<String, Class<? extends T>>> cachedClasses = Optional.empty();

    private SimpleLoadingCache<String, T> cachedInstances = new SimpleLoadingCache<>();


    public ExtensionLoader(Class<?> type) {
        this.type = type;
        // 利用本身的机制来加载ExtensionFactory类
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoaderHelper.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
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

//            if (wrap) {
//
//                List<Class<?>> wrapperClassesList = new ArrayList<>();
//                if (cachedWrapperClasses != null) {
//                    wrapperClassesList.addAll(cachedWrapperClasses);
//                    wrapperClassesList.sort(WrapperComparator.COMPARATOR);
//                    Collections.reverse(wrapperClassesList);
//                }
//
//                if (CollectionUtils.isNotEmpty(wrapperClassesList)) {
//                    for (Class<?> wrapperClass : wrapperClassesList) {
//                        Wrapper wrapper = wrapperClass.getAnnotation(Wrapper.class);
//                        if (wrapper == null
//                                || (ArrayUtils.contains(wrapper.matches(), name) && !ArrayUtils.contains(wrapper.mismatches(), name))) {
//                            instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
//                        }
//                    }
//                }
//            }
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
//        String code = new AdaptiveClassCodeGenerator(type, cachedDefaultName).generate();
//        ClassLoader classLoader = findClassLoader();
//        org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
//        return compiler.compile(code, classLoader);
        return null;
    }

    static ClassLoader findClassLoader() {
//
//        return ClassUtils.getClassLoader(ExtensionLoader.class);
        return null;
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
        if(defaultAnnotation.strategy() != null || annoStrategy != null){
            if (defaultAnnotation.strategy() != null) {
                for (String s : defaultAnnotation.strategy()){

                }
                LoadingStrategy strategy = ExtensionLoaderHelper.loadingStrategyCache.get(defaultAnnotation.strategy());
                loadDirectory0(extensionClasses, type.getName(), strategy);
            }
            if (annoStrategy != null) {

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

    private void loadResource(Map<String, Class<? extends T>> extensionClasses, ClassLoader classLoader, URL resourceURL, boolean overridden, String[] excludedPackages) {

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



    // for init loading strategy, 逻辑稍有不同
}
