package one.pkg.kfnp.shared;

import one.pkg.config.SewliaConfig;
import one.pkg.config.annotation.config.ConfigEntry;
import one.pkg.config.annotation.config.ConfigTarget;
import one.pkg.config.annotation.loader.ReadWith;
import one.pkg.config.metadata.ConfigMeta;
import one.pkg.config.metadata.DumpMeta;
import one.pkg.loader.Loader;

@ConfigEntry("krypton_fnp")
public class ModConfig {
    public static final SewliaConfig config;
    @ConfigTarget(group = "compress", value = "compressionLevel", comment = "The compression level for packets, between 1-9.")
    private static int var1 = 4;
    @ConfigTarget(group = "compress", value = "permitOversizedPackets", comment = "Permit Oversized Packets")
    private static boolean var2 = false;
    @ConfigTarget(group = "fix.issues128", value = "enabled", comment = "Fix Traffic Statistics")
    private static boolean var3 = false;
    @ConfigTarget(group = "fix.issues128", value = "sync", comment = "Run bandwidth statistics on sync thread, which is closer to Vanilla behavior.")
    private static boolean var4 = true;
    @ConfigTarget(group = "compatibility", value = "allow-wide-var-int")
    private static boolean var5 = false;
    @ConfigTarget(group = "mixin", value = "loginVT", comment = "Replace player login validation thread with virtual thread")
    private static boolean var6 = true;
    @ConfigTarget(group = "mixin", value = "textFilterVT", comment = "Replace text filter thread with virtual thread")
    private static boolean var7 = true;
    @ConfigTarget(group = "mixin", value = "utilVT", comment = "Replace download thread with virtual thread")
    private static boolean var8 = true;
    @ConfigTarget(group = "mixin", value = "bestVarLong", comment = "Optimized VarLong implementation")
    private static boolean var9 = true;
    @ConfigTarget(group = "mixin", value = "clientEncrypt", comment = "Enable new encryption optimizations on the client side")
    private static boolean var10 = true;
    @ConfigTarget(group = "mixin", value = "rconClient", comment = "Optimized RconClient implementation")
    private static boolean var11 = false;
    @ConfigTarget(group = "netty", value = "allocatorMaxOrder", comment = "Change Netty's default 16MiB memory allocation to 4MiB, as Minecraft has a 2MiB packet size limit.")
    private static int var12 = 9;
    @ConfigTarget(group = "netty", value = "happyEyeballs", comment = "Enable Happy Eyeballs (RFC 8305) for client connections to race IPv6 and IPv4.")
    private static boolean var13 = false;

    static {
        config = new SewliaConfig(ConfigMeta.of(
                ModConfig.class,
                Loader.INSTANCE.getConfigPath().resolve("krypton_fnp.yaml"))
        );
    }

    private ModConfig() {
    }

    @ReadWith("var1")
    private static void setCompressionLevel(DumpMeta dumpMeta) {
        if (!(dumpMeta.getObject() instanceof Integer))
            dumpMeta.setCancelled(true);

        int level = (Integer) dumpMeta.getObject();

        if (level > 9 || level < 1) {
            dumpMeta.setObject(4);
            dumpMeta.setCancelled(true);
        }
    }

    @ReadWith("var12")
    private static void setAllocatorMaxOrder(DumpMeta dumpMeta) {
        if (!(dumpMeta.getObject() instanceof Integer))
            dumpMeta.setCancelled(true);
        int level = (Integer) dumpMeta.getObject();
        if (level > 51 || level < 9) {
            dumpMeta.setObject(9);
            dumpMeta.setCancelled(true);
        }
    }

    public static class Compression {
        public static int getLevel() {
            return var1;
        }

        public static boolean isPermitOversizedPackets() {
            return var2;
        }
    }

    public static class Fix {
        public static class Issues128 {
            public static boolean isEnabled() {
                return var3;
            }

            public static boolean isSync() {
                return var4;
            }
        }
    }

    public static class Compatibility {
        public static boolean AllowWideVarInt() {
            return var5;
        }
    }

    public static class Mixin {
        public static boolean isLoginVT() {
            return var6;
        }

        public static boolean isTextFilterVT() {
            return var7;
        }

        public static boolean isUtilVT() {
            return var8;
        }

        public static boolean isBestVarLong() {
            return var9;
        }

        public static boolean isClientEncrypt() {
            return var10;
        }

        public static boolean isRconClient() {
            return var11;
        }
    }

    public static class Netty {
        public static int getAllocatorMaxOrder() {
            return var12;
        }

        public static boolean isHappyEyeballs() {
            return var13;
        }
    }
}
