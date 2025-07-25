package me.steinborn.krypton.mod;

import me.steinborn.krypton.mod.shared.KryptonFirstBootstrap;
import me.steinborn.krypton.mod.shared.KryptonSharedBootstrap;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod("krypton")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        fmlSetup();
        KryptonSharedBootstrap.run(FMLLoader.getDist().isClient());
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
