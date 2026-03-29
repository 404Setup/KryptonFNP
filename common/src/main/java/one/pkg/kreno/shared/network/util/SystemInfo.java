package one.pkg.kreno.shared.network.util;

public class SystemInfo {
    public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    public static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
}
