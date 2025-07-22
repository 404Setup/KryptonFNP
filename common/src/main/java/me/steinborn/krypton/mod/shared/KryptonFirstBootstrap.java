package me.steinborn.krypton.mod.shared;

import one.pkg.config.SewliaConfig;
import one.pkg.config.metadata.ConfigMeta;
import one.pkg.loader.Loader;

public class KryptonFirstBootstrap {
    public static final SewliaConfig config;

    static {
        config = new SewliaConfig(ConfigMeta.of(
                KryptonFNPModConfig.class,
                Loader.INSTANCE.getConfigPath().resolve("krypton_fnp.yaml"))
        );
    }

    public static void bootstrap() {}
}
