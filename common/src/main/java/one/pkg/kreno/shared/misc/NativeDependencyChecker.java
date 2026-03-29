package one.pkg.kreno.shared.misc;

import static one.pkg.kreno.shared.network.util.SystemInfo.IS_MAC;
import static one.pkg.kreno.shared.network.util.SystemInfo.IS_WINDOWS;

public class NativeDependencyChecker {

    public static boolean hasMsvcRedist() {
        if (!IS_WINDOWS) return true;

        try {
            System.loadLibrary("vcruntime140");
            return true;
        } catch (UnsatisfiedLinkError | SecurityException e) {
            return false;
        }
    }

    public static boolean hasOpenSSL() {
        if (IS_MAC) return true;

        String[] libNames;
        if (IS_WINDOWS) {
            libNames = new String[]{
                    "libssl-3-x64", "libssl-1_1-x64", "ssl-3-x64", "ssl-1_1-x64", "ssl", "ssleay32"
            };
        } else {
            libNames = new String[]{"ssl", "crypto"};
        }

        for (String libName : libNames) {
            try {
                System.loadLibrary(libName);
                return true;
            } catch (UnsatisfiedLinkError | SecurityException ignored) {
            }
        }

        return false;
    }
}
