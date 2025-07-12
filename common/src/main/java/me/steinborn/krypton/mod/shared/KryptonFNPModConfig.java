package me.steinborn.krypton.mod.shared;

import me.steinborn.krypton.mod.shared.config.compress.CompressionLevel;
import me.steinborn.krypton.mod.shared.config.compress.PermitOversizedPackets;
import me.steinborn.krypton.mod.shared.config.fix.KryptonIssues128;
import me.steinborn.krypton.mod.shared.config.fix.KryptonIssues128Sync;
import me.steinborn.krypton.mod.shared.config.mixin.BestVarLong;
import me.steinborn.krypton.mod.shared.config.mixin.LoginVt;
import me.steinborn.krypton.mod.shared.config.mixin.TextFilterVt;
import me.steinborn.krypton.mod.shared.config.mixin.UtilVt;
import me.steinborn.krypton.mod.shared.config.netty.AllocatorMaxOrder;
import one.pkg.config.SewliaConfig;
import one.pkg.config.annotation.config.ConfigEntry;
import one.pkg.config.annotation.config.ConfigTarget;
import one.pkg.config.metadata.ConfigMeta;
import one.pkg.loader.Loader;

@ConfigEntry(isCommonUsed = true)
public class KryptonFNPModConfig {
    public static final SewliaConfig config = new SewliaConfig(
            new ConfigMeta(KryptonFNPModConfig.class,
                    Loader.INSTANCE.getConfigPath().resolve("krypton_fnp.yaml").toString()
            )
    );

    @ConfigTarget
    public final static int compressionLevel = 4;

    public static void init() {
    }
}
