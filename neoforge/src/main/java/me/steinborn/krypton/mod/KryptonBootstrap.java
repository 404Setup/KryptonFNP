package me.steinborn.krypton.mod;

import me.steinborn.krypton.mod.shared.KryptonFNPModConfig;
import me.steinborn.krypton.mod.shared.KryptonSharedBootstrap;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.xstopho.resourceconfigapi.api.ConfigRegistry;

@Mod("krypton")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        fmlSetup();
        ConfigRegistry.register(KryptonFNPModConfig.class, KryptonSharedBootstrap.MOD_ID);
        KryptonSharedBootstrap.run(FMLLoader.getDist().isClient());
        KryptonSharedBootstrap.setVersion(FMLLoader.getLoadingModList().getModFileById("krypton").versionString());
    }

    // This is a deliberate check.
    protected void fmlSetup() {
        checkMod("arclight");
        checkMod("mohist");
        try {
            Class.forName("org.bukkit.advancement.Advancement");
            throw new SecurityException("Unsupported mod detected: bukkit");
        } catch (ClassNotFoundException ignored) {
        }
    }

    protected void checkMod(String modid) {
        if (FMLLoader.getLoadingModList().getModFileById(modid) != null)
            throw new SecurityException("Unsupported mod detected: " + modid);
    }
}
