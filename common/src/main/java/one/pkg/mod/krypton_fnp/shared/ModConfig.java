package one.pkg.mod.krypton_fnp.shared;

import one.pkg.config.SewliaConfig;
import one.pkg.config.annotation.config.ConfigEntry;
import one.pkg.config.annotation.config.ConfigTarget;
import one.pkg.config.metadata.ConfigMeta;

@ConfigEntry("krypton_fnp")
public class ModConfig {
    public static final SewliaConfig config;
    @ConfigTarget(group = "compress", value = "compressionLevel", comment = "The compression level for packets, between 1-9.")
    private static int var1 = 4;
    @ConfigTarget(group = "compatibility", value = "allow-wide-var-int")
    private static boolean var2 = false;

    static {
        config = new SewliaConfig(ConfigMeta.of(
                ModConfig.class,
                ModSharedBootstrap.CONFIG_PATH.resolve("krypton_fnp.yaml"))
        );
    }

    private ModConfig() {
    }

    public static class Compression {
        public static int getLevel() {
            return var1;
        }
    }

    public static class Compatibility {
        public static boolean AllowWideVarInt() {
            return var2;
        }
    }
}