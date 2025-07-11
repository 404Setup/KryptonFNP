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
import one.pkg.loader.Loader;

public class KryptonFNPModConfig {
    // It's a very simple thing that Forge can do but NeoForge can't, so I need to add all the configurations manually.
    public static final SewliaConfig config = new SewliaConfig(
            "me.steinborn.krypton.mod.shared.config",
            Loader.INSTANCE.getConfigPath().resolve("krypton_fnp.yaml").toString(),
            null,
            CompressionLevel.class,
            PermitOversizedPackets.class,
            KryptonIssues128.class,
            KryptonIssues128Sync.class,
            BestVarLong.class,
            LoginVt.class,
            TextFilterVt.class,
            UtilVt.class,
            AllocatorMaxOrder.class);

    public static void init() {
    }
}
