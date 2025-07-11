package one.pkg.loader;

import java.util.ServiceLoader;

public class Loader {
    public static final LoaderImpl INSTANCE = load(LoaderImpl.class);

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}
