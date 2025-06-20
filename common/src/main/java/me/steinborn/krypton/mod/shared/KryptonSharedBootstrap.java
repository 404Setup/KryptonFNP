package me.steinborn.krypton.mod.shared;

import com.velocitypowered.natives.util.Natives;
import org.slf4j.Logger;

public class KryptonSharedBootstrap {
    public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(KryptonSharedBootstrap.class);

    static {
        // By default, Netty allocates 16MiB arenas for the PooledByteBufAllocator. This is too much
        // memory for Minecraft, which imposes a maximum packet size of 2MiB! We'll use 4MiB as a more
        // sane default.
        //
        // Note: io.netty.allocator.pageSize << io.netty.allocator.maxOrder is the formula used to
        // compute the chunk size. We lower maxOrder from its default of 11 to 9. (We also use a null
        // check, so that the user is free to choose another setting if need be.)
        if (System.getProperty("io.netty.allocator.maxOrder") == null) {
            System.setProperty("io.netty.allocator.maxOrder", "9");
        }
    }

    public static String getVersion() {
        return System.getProperty("krypton.version");
    }

    public static void setVersion(String version) {
        if (System.getProperty("krypton.version") == null) {
            System.setProperty("krypton.version", version);
        }
    }

    public static void run(boolean client) {
        if (!client) {
            LOGGER.info("Krypton is now accelerating your Minecraft server's networking stack \uD83D\uDE80");
        } else {
            LOGGER.info("Krypton is now accelerating your Minecraft client's networking stack \uD83D\uDE80");
            LOGGER.info("Note that Krypton is most effective on servers, not the client.");
        }
        LOGGER.info("Compression will use {}, encryption will use {}", Natives.compress.getLoadedVariant(), Natives.cipher.getLoadedVariant());
    }
}