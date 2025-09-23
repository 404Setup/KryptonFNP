package one.pkg.mod.krypton_fnp;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import one.pkg.mod.krypton_fnp.shared.ModConfig;
import one.pkg.mod.krypton_fnp.shared.ModSharedBootstrap;

@Mod("krypton_fnp")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        ModSharedBootstrap.CONFIG_PATH = FMLPaths.CONFIGDIR.get();
        ModConfig.config.addConfigurations();
        ModSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}
