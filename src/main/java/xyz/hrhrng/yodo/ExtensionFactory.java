package xyz.hrhrng.yodo;

public interface ExtensionFactory {
    <T> T getExtension(Class<T> type, String name);
}
