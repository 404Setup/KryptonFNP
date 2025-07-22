package me.steinborn.krypton.mod.shared;

import one.pkg.config.annotation.config.ConfigEntry;
import one.pkg.config.annotation.config.ConfigTarget;
import one.pkg.config.annotation.loader.ReadWith;
import one.pkg.config.metadata.DumpMeta;

@ConfigEntry
public class KryptonFNPModConfig {
    @ConfigTarget(group = "compress", comment = "The compression level for packets, between 1-9.")
    private static int compressionLevel = 4;
    @ConfigTarget(group = "compress", comment = "Permit Oversized Packets")
    private static boolean permitOversizedPackets = false;

    @ConfigTarget(group = "fix.issues128", value = "enabled", comment = "Fix Traffic Statistics")
    private static boolean issues128 = false;
    @ConfigTarget(group = "fix.issues128", value = "sync", comment = "Run bandwidth statistics on sync thread, which is closer to Vanilla behavior.")
    private static boolean issues128Sync = true;

    @ConfigTarget(group = "mixin", comment = "Replace player login validation thread with virtual thread")
    private static boolean loginVT = true;
    @ConfigTarget(group = "mixin", comment = "Replace text filter thread with virtual thread")
    private static boolean textFilterVT = true;
    @ConfigTarget(group = "mixin", comment = "Replace download thread with virtual thread")
    private static boolean utilVT = true;
    @ConfigTarget(group = "mixin", comment = "Optimized VarLong implementation")
    private static boolean bestVarLong = true;


    @ConfigTarget(group = "netty", comment = "Change Netty's default 16MiB memory allocation to 4MiB, as Minecraft has a 2MiB packet size limit.")
    private static int allocatorMaxOrder = 9;

    private KryptonFNPModConfig() {
    }

    public static int getCompressionLevel() {
        return compressionLevel;
    }

    @ReadWith("compressionLevel")
    private static void setCompressionLevel(DumpMeta dumpMeta) {
        if (!(dumpMeta.getObject() instanceof Integer))
            dumpMeta.setCancelled(true);

        int level = (Integer) dumpMeta.getObject();

        if (level > 9 || level < 1) {
            dumpMeta.setObject(4);
            dumpMeta.setCancelled(true);
        }
    }

    public static boolean isPermitOversizedPackets() {
        return permitOversizedPackets;
    }

    public static boolean isIssues128() {
        return issues128;
    }

    public static boolean isIssues128Sync() {
        return issues128Sync;
    }

    public static boolean isLoginVT() {
        return loginVT;
    }

    public static boolean isTextFilterVT() {
        return textFilterVT;
    }

    public static boolean isUtilVT() {
        return utilVT;
    }

    public static boolean isBestVarLong() {
        return bestVarLong;
    }

    public static int getAllocatorMaxOrder() {
        return allocatorMaxOrder;
    }

    @ReadWith("allocatorMaxOrder")
    private static void setAllocatorMaxOrder(DumpMeta dumpMeta) {
        if (!(dumpMeta.getObject() instanceof Integer))
            dumpMeta.setCancelled(true);
        int level = (Integer) dumpMeta.getObject();
        if (level > 51 || level < 9) {
            dumpMeta.setObject(9);
            dumpMeta.setCancelled(true);
        }

    }
}
