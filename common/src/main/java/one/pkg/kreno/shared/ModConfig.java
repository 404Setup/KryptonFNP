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
    private static boolean var3 = false;
    @ConfigTarget(group = "fix.issues128", value = "sync", comment = "Run bandwidth statistics on sync thread, which is closer to Vanilla behavior.")
    private static boolean var4 = true;
    @ConfigTarget(group = "compatibility", value = "allow-wide-var-int")
    private static boolean wideVarInt = false;
    @ConfigTarget(group = "mixin", value = "loginVT", comment = "Replace player login validation thread with virtual thread")
    private static boolean loginVt = true;
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
    @ConfigTarget(group = "mixin", value = "serverEntityMoveOpt", comment = "Skips sending movement packets if the entity hasn't moved, and downgrades position+rotation packets to just rotation if the entity only turned")
    private static boolean serverEntityMoveOpt = false;
    @ConfigTarget(group = "mixin", value = "connectionMicroOpt", comment = "Reduces object allocation and lock contention in the Connection class")
    private static boolean connectionMicroOpt = true;
    @ConfigTarget(group = "mixin", value = "particlePacketOpt", comment = "Reduces some potentially useless particle packets. This configuration only takes effect on the server side.")
    private static boolean particlePacketOpt = true;
    @ConfigTarget(group = "mixin", value = "trackedEntityOpt", comment = "Optimizes entity packet broadcasting and integrates with server-side entity culling")
    private static boolean trackedEntityOpt = true;
    @ConfigTarget(group = "netty", value = "allocatorMaxOrder", comment = "Change Netty's default 16MiB memory allocation to 4MiB, as Minecraft has a 2MiB packet size limit.")
    @Range(min = 9, max = 51)
    @DisplayMode(EntryMode.SLIDER)
    private static int nettyAllocatorMaxOrder = 9;
    @ConfigTarget(group = "netty", value = "happyEyeballs", comment = "Enable Happy Eyeballs (RFC 8305) for client connections to race IPv6 and IPv4. May cause some servers (like Velocity) to temporarily refuse connections.")
    private static boolean nettyHe = false;
    @ConfigTarget(group = "gui", value = "oreui", comment = "Replace Minecraft style KReno UI with a newly designed OreUI")
    private static boolean guiUseOreUITheme = false;

    @ConfigTarget(group = "culling", value = "particle", comment = "Smart particle culling on server side")
    private static boolean cullingParticle = true;
    @ConfigTarget(group = "culling", value = "entity", comment = "Smart entity culling on server side")
    private static boolean cullingEntity = true;
    @ConfigTarget(group = "culling", value = "block", comment = "Smart block/block entity culling on server side")
    private static boolean cullingBlock = true;
    @ConfigTarget(group = "culling", value = "chunk_block", comment = "Replaces completely hidden blocks in chunk packets with air to save bandwidth")
    private static boolean cullingChunkBlock = true;
    @ConfigTarget(group = "culling", value = "chunk_light", comment = "Treat light sections whose data array is fully zero as empty to skip 2KiB payload per section in ClientboundLevelChunkWithLightPacket / ClientboundLightUpdatePacket")
    private static boolean cullingChunkLight = true;
    @ConfigTarget(group = "culling", value = "asyncMode", comment = "Asynchronous execution mode for Cuttings system")
    private static boolean cullingAsyncMode = true;

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
        if (!(dumpMeta.getObject() instanceof Integer))
            dumpMeta.setCancelled(true);

        int level = (Integer) dumpMeta.getObject();

        if (level > 9 || level < 1) {
            dumpMeta.setObject(4);
            dumpMeta.setCancelled(true);
        }
    }

    @ReadWith("nettyAllocatorMaxOrder")
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
            return compressLevel;
        }

        public static boolean isPermitOversizedPackets() {
            return compressPop;
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
            return wideVarInt;
        }
    }

    public static class Mixin {
        public static boolean isLoginVT() {
            return loginVt;
        }

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

        public static boolean isConnectionMicroOpt() {
            return connectionMicroOpt;
        }

        public static boolean isParticlePacketOpt() {
            return particlePacketOpt;
        }

        public static boolean isTrackedEntityOpt() {
            return trackedEntityOpt;
        }
    }

    public static class Netty {
        public static int getAllocatorMaxOrder() {
            return nettyAllocatorMaxOrder;
        }

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

        public static boolean isBlockEnabled() {
            return !JavaLoader.INSTANCE.isClient() &&cullingBlock;
        }

        public static boolean isChunkBlockCullingEnabled() {
            return !JavaLoader.INSTANCE.isClient() &&cullingChunkBlock;
        }

        public static boolean isChunkLightCullingEnabled() {
            return !JavaLoader.INSTANCE.isClient() &&cullingChunkLight;
        }

        public static boolean isAsyncMode() {
            return cullingAsyncMode;
        }
    }
}
