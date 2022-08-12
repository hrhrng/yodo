package xyz.hrhrng.yodo.testclass;

import org.junit.Test;
import xyz.hrhrng.yodo.ExtensionLoader;
import xyz.hrhrng.yodo.helper.ExtensionLoaderHelper;


public class MainTest {

    // 测试基本功能
    @Test
    public void test1 () {
        Class<Robot> c = Robot.class;
        ExtensionLoader<Robot> extensionLoader = ExtensionLoaderHelper.getExtensionLoader(c);
        Robot optimusPrime = extensionLoader.getExtension("bumblebee");
        assert optimusPrime.hello() == "I am Bumblebee!";
    }
    // 多值映射
    @Test
    public void test2 () {
        Class<Robot> c = Robot.class;
        ExtensionLoader<Robot> extensionLoader = ExtensionLoaderHelper.getExtensionLoader(c);
        Robot du = extensionLoader.getExtension("du1");
        Robot du2 = extensionLoader.getExtension("du2");
        assert du.hello() == "I am DamnYou!" && du == du2;
    }

    // 自定义策略

    // SPI 自定义策略

    // Strategy 自定义策略

}
