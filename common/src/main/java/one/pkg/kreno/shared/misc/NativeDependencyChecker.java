package one.pkg.kreno.shared.misc;

import java.io.File;
import java.nio.file.Paths;

import static one.pkg.kreno.shared.network.util.SystemInfo.IS_MAC;
import static one.pkg.kreno.shared.network.util.SystemInfo.IS_WINDOWS;

// TODO: Add a warning dialog (for client)
public class NativeDependencyChecker {

    // Most PCs should include MSVC C++.
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

        if (IS_WINDOWS) {
            String systemDrive = System.getenv("SystemDrive");
            if (systemDrive == null || systemDrive.isEmpty()) {
                systemDrive = "C:";
            }

            String[] possiblePaths = {
                    System.getenv("OPENSSL_DIR"),
                    systemDrive + "\\Program Files\\OpenSSL-Win64\\bin",
                    systemDrive + "\\Program Files\\OpenSSL\\bin",
                    systemDrive + "\\OpenSSL-Win64\\bin",
                    systemDrive + "\\OpenSSL\\bin"
            };

            for (String basePath : possiblePaths) {
                if (basePath == null || basePath.isEmpty()) continue;

                for (String libName : new String[]{"libssl-3-x64.dll", "libssl-1_1-x64.dll"}) {
                    try {
                        String fullPath = Paths.get(basePath, libName).toString();
                        if (new File(fullPath).exists()) {
                            System.load(fullPath);
                            return true;
                        }
                    } catch (UnsatisfiedLinkError | SecurityException ignored) {
                    }
                }
            }
        }

        return false;
    }
}
