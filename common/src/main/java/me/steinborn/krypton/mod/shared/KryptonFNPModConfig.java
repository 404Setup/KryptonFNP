package me.steinborn.krypton.mod.shared;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;
import de.bsommerfeld.jshepherd.annotation.PostInject;
import de.bsommerfeld.jshepherd.core.ConfigurablePojo;
import de.bsommerfeld.jshepherd.core.ConfigurationLoader;
import one.pkg.loader.Loader;

import java.nio.file.Path;

public class KryptonFNPModConfig extends ConfigurablePojo<KryptonFNPModConfig> {
    public static final KryptonFNPModConfig INSTANCE = init();

    @Key("compress-compressionLevel")
    @Comment("The compression level for packets, between 1-9.")
    private int compressionLevel = 4;
    @Key("compress-permitOversizedPackets")
    @Comment("Permit Oversized Packets")
    private boolean permitOversizedPackets = false;
    @Key("fix-issues128-enabled")
    @Comment("Fix Traffic Statistics")
    private boolean issues128 = false;
    @Key("fix-issues128-sync")
    @Comment("Run bandwidth statistics on sync thread, which is closer to Vanilla behavior.")
    private boolean issues128Sync = true;
    @Key("mixin-loginVT")
    @Comment("Replace player login validation thread with virtual thread")
    private boolean loginVT = true;
    @Key("mixin-textFilterVT")
    @Comment("Replace text filter thread with virtual thread")
    private boolean textFilterVT = true;
    @Key("mixin-utilVT")
    @Comment("Replace download thread with virtual thread")
    private boolean utilVT = true;
    @Key("mixin-bestVarLong")
    @Comment("Optimized VarLong implementation")
    private boolean bestVarLong = true;
    @Key("netty-allocatorMaxOrder")
    @Comment("Change Netty's default 16MiB memory allocation to 4MiB, as Minecraft has a 2MiB packet size limit.")
    private int allocatorMaxOrder = 9;

    public KryptonFNPModConfig() {
    }

    private static KryptonFNPModConfig init() {
        Path configFile = Loader.INSTANCE.getConfigPath().resolve("krypton_fnp.yaml");

        KryptonFNPModConfig config = ConfigurationLoader.load(configFile, KryptonFNPModConfig::new);
        config.save();
        config.reload();

        return config;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public boolean isPermitOversizedPackets() {
        return permitOversizedPackets;
    }

    public boolean isIssues128() {
        return issues128;
    }

    public boolean isIssues128Sync() {
        return issues128Sync;
    }

    public boolean isLoginVT() {
        return loginVT;
    }

    public boolean isTextFilterVT() {
        return textFilterVT;
    }

    public boolean isUtilVT() {
        return utilVT;
    }

    public boolean isBestVarLong() {
        return bestVarLong;
    }

    public int getAllocatorMaxOrder() {
        return allocatorMaxOrder;
    }

    @PostInject
    private void setCompressionLevel() {
        if (compressionLevel > 9 || compressionLevel < 1)
            compressionLevel = 4;
    }

    @PostInject
    private void setAllocatorMaxOrder() {
        if (allocatorMaxOrder > 51 || allocatorMaxOrder < 9)
            allocatorMaxOrder = 9;

    }
}
