package one.pkg.kreno.shared;

import com.velocitypowered.natives.util.Natives;
import one.pkg.libsl.api.loader.JavaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModSharedBootstrap {
    public static final String MOD_ID = "kreno";
    public static final Logger LOGGER = LoggerFactory.getLogger("KryptonReno ModSharedBootstrap");

    public static void run() {
        if (!JavaLoader.INSTANCE.isClient()) {
            LOGGER.info("KryptonReno is now accelerating your Minecraft server's networking stack \uD83D\uDE80");
        } else {
            LOGGER.info("KryptonReno is now accelerating your Minecraft client's networking stack \uD83D\uDE80");
            LOGGER.info("Note that KryptonReno is most effective on servers, not the client.");
        }
        LOGGER.info("Compression will use {}, encryption will use {}", Natives.compress.getLoadedVariant(), Natives.cipher.getLoadedVariant());


    }
}
