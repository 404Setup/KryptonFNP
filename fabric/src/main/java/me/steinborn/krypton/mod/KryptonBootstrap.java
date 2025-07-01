package me.steinborn.krypton.mod;

import me.steinborn.krypton.mod.shared.KryptonFNPModConfig;
import me.steinborn.krypton.mod.shared.KryptonSharedBootstrap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.xstopho.resourceconfigapi.api.ConfigRegistry;

public class KryptonBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        safetyCheck();
        ConfigRegistry.register(KryptonFNPModConfig.class, KryptonSharedBootstrap.MOD_ID);
        KryptonSharedBootstrap.run(FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT));
    }

    // This is a deliberate check.
    protected void safetyCheck() {
        try {
            Class.forName("org.bukkit.advancement.Advancement");
            throw new SecurityException("Unsupported mod detected: bukkit");
        } catch (ClassNotFoundException ignored) {
        }
    }
}
