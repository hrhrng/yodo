package xyz.hrhrng.yodo;

import org.junit.Test;
import xyz.hrhrng.yodo.helper.ExtensionLoaderHelper;

import java.awt.*;

public class MainTest {

    @Test
    public void test1 () {
        ExtensionLoader<Robot> extensionLoader =
                ExtensionLoaderHelper.getExtensionLoader(Robot.class);
    }
    @Test
    public void test2 () {

        System.out.println(this.getClass().getName());
    }

}
