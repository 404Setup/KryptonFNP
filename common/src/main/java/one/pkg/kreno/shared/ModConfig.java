package one.pkg.kreno.shared;

import one.pkg.config.SewliaConfig;
import one.pkg.config.annotation.config.ConfigEntry;
import one.pkg.config.annotation.config.ConfigTarget;
import one.pkg.config.annotation.loader.ReadWith;
import one.pkg.config.metadata.ConfigMeta;
import one.pkg.config.metadata.DumpMeta;
import one.pkg.libsl.api.loader.JavaLoader;
import one.pkg.libsl.api.ui.seeui.EntryMode;
import one.pkg.libsl.api.ui.seeui.annotations.DisplayMode;
import one.pkg.libsl.api.ui.seeui.annotations.Range;

@ConfigEntry("kreno")
public class ModConfig {
    public static final SewliaConfig config;
    @ConfigTarget(group = "compress", value = "compressionLevel", comment = "The compression level for packets, between 1-9.")
    @Range(min = 1, max = 9)
    @DisplayMode(EntryMode.SLIDER)
    private static int compressLevel = 4;
    @ConfigTarget(group = "compress", value = "permitOversizedPackets", comment = "Permit Oversized Packets")
    private static boolean compressPop = false;
    @ConfigTarget(group = "fix.issues128", value = "enabled", comment = "Fix Traffic Statistics")
    private static boolean fixIssues128Enabled = false;
    @ConfigTarget(group = "fix.issues128", value = "sync", comment = "Run bandwidth statistics on sync thread, which is closer to Vanilla behavior.")
    private static boolean fixIssues128Sync = true;
    @ConfigTarget(group = "compatibility", value = "allow-wide-var-int", comment = "Allow non-standard frame lengths encoded as four- or five-byte VarInts")
    private static boolean wideVarInt = false;
    @ConfigTarget(group = "mixin", value = "textFilterVT", comment = "Replace text filter thread with virtual thread")
    private static boolean textFilterVT = true;
    @ConfigTarget(group = "mixin", value = "utilVT", comment = "Replace download thread with virtual thread")
    private static boolean utilVt = true;
    @ConfigTarget(group = "mixin", value = "bestVarLong", comment = "Optimized VarLong implementation")
    private static boolean bestVarLong = true;
    @ConfigTarget(group = "mixin", value = "clientEncrypt", comment = "Enable new encryption optimizations on the client side")
    private static boolean clientEncrypt = true;
    @ConfigTarget(group = "mixin", value = "rconClient", comment = "Optimized RconClient implementation")
    private static boolean rconClient = false;
    @ConfigTarget(group = "mixin", value = "serverEntityMoveOpt", comment = "Skips motion updates only when both velocities encode to zero on the client")
    private static boolean serverEntityMoveOpt = false;
    @ConfigTarget(group = "mixin", value = "packetProcessorOpt", comment = "Halves concurrent queue operations when draining queued packets on the main thread")
    private static boolean packetProcessorOpt = true;
    @ConfigTarget(group = "mixin", value = "particlePacketOpt", comment = "Reduces some potentially useless particle packets. This configuration only takes effect on the server side.")
    private static boolean particlePacketOpt = true;
    @ConfigTarget(group = "mixin", value = "trackedEntityOpt", comment = "Optimizes entity packet broadcasting and integrates with server-side entity culling")
    private static boolean trackedEntityOpt = true;
    @ConfigTarget(group = "netty", value = "happyEyeballs", comment = "Enable Happy Eyeballs (RFC 8305) for client connections to race IPv6 and IPv4. May cause some servers (like Velocity) to temporarily refuse connections.")
    private static boolean nettyHe = false;
    @ConfigTarget(group = "gui", value = "oreui", comment = "Replace Minecraft style KReno UI with a newly designed OreUI")
    private static boolean guiUseOreUITheme = false;

    @ConfigTarget(group = "culling", value = "particle", comment = "Smart particle culling on server side")
    private static boolean cullingParticle = true;
    @ConfigTarget(group = "culling", value = "entity", comment = "Cull occluded display and hanging entities on dedicated servers")
    private static boolean cullingEntity = true;

    static {
        config = new SewliaConfig(ConfigMeta.of(
                ModConfig.class,
                JavaLoader.INSTANCE.getConfigPath().resolve("kreno.yaml"))
        );
    }

    private ModConfig() {
    }

    @ReadWith("compressLevel")
    private static void setCompressionLevel(DumpMeta dumpMeta) {
        if (!(dumpMeta.getObject() instanceof Integer)) {
            dumpMeta.setCancelled(true);
            return;
        }

        int level = (Integer) dumpMeta.getObject();

        if (level > 9 || level < 1) {
            dumpMeta.setObject(4);
            dumpMeta.setCancelled(true);
        }
    }

    public static class Compression {
        public static int getLevel() {
            return compressLevel;
        }

        public static boolean isPermitOversizedPackets() {
            return compressPop;
        }
    }

    public static class Fix {
        public static class Issues128 {
            public static boolean isEnabled() {
                return fixIssues128Enabled;
            }

            public static boolean isSync() {
                return fixIssues128Sync;
            }
        }
    }

    public static class Compatibility {
        public static boolean isAllowWideVarInt() {
            return wideVarInt;
        }
    }

    public static class Mixin {
        public static boolean isTextFilterVT() {
            return textFilterVT;
        }

        public static boolean isUtilVT() {
            return utilVt;
        }

        public static boolean isBestVarLong() {
            return bestVarLong;
        }

        public static boolean isClientEncrypt() {
            return clientEncrypt;
        }

        public static boolean isRconClient() {
            return rconClient;
        }

        public static boolean isServerEntityMoveOpt() {
            return serverEntityMoveOpt;
        }

        public static boolean isPacketProcessorOpt() {
            return packetProcessorOpt;
        }

        public static boolean isParticlePacketOpt() {
            return !JavaLoader.INSTANCE.isClient() && particlePacketOpt;
        }

        public static boolean isTrackedEntityOpt() {
            return trackedEntityOpt;
        }
    }

    public static class Netty {
        public static boolean isHappyEyeballs() {
            return nettyHe;
        }
    }

    public static class GUI {
        public static boolean isOreUI() {
            return guiUseOreUITheme;
        }
    }

    public static class Culling {
        public static boolean isParticleEnabled() {
            return !JavaLoader.INSTANCE.isClient() && cullingParticle;
        }

        public static boolean isEntityEnabled() {
            return !JavaLoader.INSTANCE.isClient() && cullingEntity;
        }
    }

}
