package one.pkg.kfnp;

import one.pkg.kfnp.shared.KryptonSharedBootstrap;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod("krypton_fnp")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        KryptonSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}
