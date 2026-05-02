package one.pkg.kreno.shared;

import one.pkg.config.SewliaConfig;
import one.pkg.config.annotation.config.ConfigEntry;
import one.pkg.config.annotation.config.ConfigTarget;
import one.pkg.config.metadata.ConfigMeta;
import one.pkg.loader.Loader;

@ConfigEntry("krypton_fnp")
public class ModConfig {
    public static final SewliaConfig config;
    @ConfigTarget(group = "compress", value = "compressionLevel", comment = "The compression level for packets, between 1-9.")
    private static int var1 = 4;
    @ConfigTarget(group = "compress", value = "permitOversizedPackets", comment = "Permit Oversized Packets")
    private static boolean var2 = false;
    @ConfigTarget(group = "mixin", value = "clientEncrypt", comment = "Enable new encryption optimizations on the client side")
    private static boolean var3 = true;
    @ConfigTarget(group = "mixin", value = "rconClient", comment = "Optimized RconClient implementation")
    private static boolean var4 = false;
    @ConfigTarget(group = "mixin", value = "bestVarLong", comment = "Optimized VarLong implementation")
    private static boolean var5 = true;
    @ConfigTarget(group = "mixin", value = "bestVarInt", comment = "Optimized VarInt implementation")
    private static boolean var6 = true;
    @ConfigTarget(group = "mixin", value = "bestUtf", comment = "Optimized Utf implementation")
    private static boolean var7 = true;

    static {
        config = new SewliaConfig(ConfigMeta.of(
                ModConfig.class,
                Loader.INSTANCE.getConfigPath().resolve("krypton_fnp.yaml"))
        );
    }

    private ModConfig() {
    }

    public static class Compression {
        public static int getLevel() {
            return var1;
        }

        public static boolean isPermitOversizedPackets() {
            return var2;
        }
    }

    public static class Mixin {
        public static boolean isClientEncrypt() {
            return var3;
        }

        public static boolean isRconClient() {
            return var4;
        }

        public static boolean isBestVarLong() {
            return var5;
        }

        public static boolean isBestVarInt() {
            return var6;
        }

        public static boolean isBestUtf() {
            return var7;
        }
    }
}