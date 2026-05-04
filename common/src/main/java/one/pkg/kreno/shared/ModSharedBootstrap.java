package one.pkg.kreno.shared;

import com.velocitypowered.natives.util.Natives;
import one.pkg.kreno.shared.misc.NativeDependencyChecker;
import one.pkg.libsl.api.loader.JavaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static one.pkg.kreno.shared.network.util.SystemInfo.IS_MAC;
import static one.pkg.kreno.shared.network.util.SystemInfo.IS_WINDOWS;

public class ModSharedBootstrap {
    public static final String MOD_ID = "kreno";
    public static final Logger LOGGER = LoggerFactory.getLogger("KryptonReno ModSharedBootstrap");
    public static final boolean HAS_NATIVE_MSVC = NativeDependencyChecker.hasMsvcRedist();
    public static final boolean HAS_NATIVE_OSSL = NativeDependencyChecker.hasOpenSSL();

    static {
        // By default, Netty allocates 16MiB arenas for the PooledByteBufAllocator. This is too much
        // memory for Minecraft, which imposes a maximum packet size of 2MiB! We'll use 4MiB as a more
        // sane default.
        //
        // Note: io.netty.allocator.pageSize << io.netty.allocator.maxOrder is the formula used to
        // compute the chunk size. We lower maxOrder from its default of 11 to 9. (We also use a null
        // check, so that the user is free to choose another setting if need be.)
        if (System.getProperty("io.netty.allocator.maxOrder") == null) {
            System.setProperty("io.netty.allocator.maxOrder", String.valueOf(ModConfig.Netty.getAllocatorMaxOrder()));
        }
    }

    public static void run() {
        if (IS_WINDOWS && !HAS_NATIVE_MSVC) {
            LOGGER.error("/////////////////////////// Reno Failed ///////////////////////////");
            LOGGER.error("Microsoft Visual C++ Redistributable was not detected. Have you installed it correctly?");
            LOGGER.error("If you have not installed it, please install it from the official Microsoft webpage: {}",
                    "https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170#latest-supported-redistributable-version");
            LOGGER.error("///////////////////////////////////////////////////////////////////");
        }
        if (!IS_MAC && !HAS_NATIVE_OSSL) {
            String osslName = IS_WINDOWS ? "OpenSSL-Win32" : "OpenSSL";
            LOGGER.error("/////////////////////////// Reno Failed ///////////////////////////");
            LOGGER.error("{} not detected. Have you not installed {},", osslName, osslName);
            LOGGER.error("or have you not added the {} dynamic-link libraries to your PATH environment?", osslName);
            LOGGER.error("If you have not installed {}, please install it as follows:", osslName);
            LOGGER.error("{}", IS_WINDOWS ?
                    "Download: https://slproweb.com/products/Win32OpenSSL.html or https://github.com/openssl/openssl/wiki/Binaries" : "Install Command: `apt install openssl`");
            LOGGER.error("///////////////////////////////////////////////////////////////////");
        }

        if (!JavaLoader.INSTANCE.isClient()) {
            LOGGER.info("KryptonReno is now accelerating your Minecraft server's networking stack \uD83D\uDE80");
        } else {
            LOGGER.info("KryptonReno is now accelerating your Minecraft client's networking stack \uD83D\uDE80");
            LOGGER.info("Note that KryptonReno is most effective on servers, not the client.");
        }
        LOGGER.info("Compression will use {}, encryption will use {}", Natives.compress.getLoadedVariant(), Natives.cipher.getLoadedVariant());


    }
}