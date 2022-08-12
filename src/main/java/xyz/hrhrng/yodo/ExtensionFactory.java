package xyz.hrhrng.yodo;

import xyz.hrhrng.yodo.annotation.SPI;

@SPI
public interface ExtensionFactory {
    <T> T getExtension(Class<T> type, String name);
}
