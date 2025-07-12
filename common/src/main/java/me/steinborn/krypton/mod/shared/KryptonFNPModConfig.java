package me.steinborn.krypton.mod.shared;

import one.pkg.config.SewliaConfig;
import one.pkg.config.annotation.config.ConfigEntry;
import one.pkg.config.annotation.config.ConfigTarget;
import one.pkg.config.annotation.loader.ReadWith;
import one.pkg.config.metadata.ConfigMeta;
import one.pkg.config.metadata.TempMeta;
import one.pkg.loader.Loader;

@ConfigEntry(isCommonUsed = true)
public class KryptonFNPModConfig {
    public static final SewliaConfig config = new SewliaConfig(
            new ConfigMeta(KryptonFNPModConfig.class,
                    Loader.INSTANCE.getConfigPath().resolve("krypton_fnp.yaml").toString()
            )
    );

    @ConfigTarget(group = "compress")
    public final static int compressionLevel = 4;

    @ConfigTarget(group = "compress", reload = false)
    public final static boolean permitOversizedPackets = false;

    @ConfigTarget(group = "fix")
    public final static boolean issues128 = false;
    @ConfigTarget(group = "fix")
    public final static boolean issues128Sync = true;

    @ConfigTarget(group = "mixin", reload = false)
    public final static boolean loginVT = true;
    @ConfigTarget(group = "mixin", reload = false)
    public final static boolean textFilterVT = true;
    @ConfigTarget(group = "mixin", reload = false)
    public final static boolean utilVT = true;
    @ConfigTarget(group = "mixin", reload = false)
    public final static boolean bestVarLong = true;

    @ConfigTarget(group = "netty")
    public final static int allocatorMaxOrder = 9;

    @ReadWith("compress.compressionLevel")
    public static void setCompressionLevel(TempMeta meta) {
        if (meta.getObject() instanceof Integer value) {
            if (value > 9 || value < 1)
                meta.setObject(4);
        } else {
            meta.setCancelled(true);
        }
    }

    @ReadWith("netty.allocatorMaxOrder")
    public static void setAllocatorMaxOrder(TempMeta meta) {
        if (meta.getObject() instanceof Integer value) {
            if (value > 51 || value < 9) {
                meta.setObject(9);
            }
        } else {
            meta.setCancelled(true);
        }
    }

    public static void init() {
    }
}
