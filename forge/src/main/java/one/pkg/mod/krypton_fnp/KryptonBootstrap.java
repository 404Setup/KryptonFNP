package one.pkg.mod.krypton_fnp;

import net.minecraftforge.fml.loading.FMLPaths;
import one.pkg.mod.krypton_fnp.shared.ModSharedBootstrap;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod("krypton_fnp")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        ModSharedBootstrap.CONFIG_PATH = FMLPaths.CONFIGDIR.get();
        ModSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}
