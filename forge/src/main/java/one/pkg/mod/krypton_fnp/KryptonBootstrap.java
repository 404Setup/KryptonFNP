package one.pkg.mod.krypton_fnp;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import one.pkg.mod.krypton_fnp.shared.ModSharedBootstrap;

@Mod("krypton_fnp")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        ModSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}
