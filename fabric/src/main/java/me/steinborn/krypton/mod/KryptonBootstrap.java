package me.steinborn.krypton.mod;

import me.steinborn.krypton.mod.shared.KryptonSharedBootstrap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class KryptonBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        safetyCheck();
        KryptonSharedBootstrap.run(FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT));
        KryptonSharedBootstrap.setVersion(FabricLoader.getInstance().getModContainer("krypton").get().getMetadata().getVersion().getFriendlyString());
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
