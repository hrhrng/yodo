package xyz.hrhrng.yodo;

public class SimpleStrategy implements LoadingStrategy{


    @Override
    public String directory() {
        return null;
    }

    @Override
    public boolean preferExtensionClassLoader() {
        return LoadingStrategy.super.preferExtensionClassLoader();
    }

    @Override
    public String[] excludedPackages() {
        return LoadingStrategy.super.excludedPackages();
    }

    @Override
    public boolean overridden() {
        return LoadingStrategy.super.overridden();
    }
}
