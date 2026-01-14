package one.pkg.kfnp.shared.network.util;

public class SystemInfo {
    public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
}
