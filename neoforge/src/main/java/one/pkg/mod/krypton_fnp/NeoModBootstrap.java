package one.pkg.mod.krypton_fnp;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import one.pkg.loader.FMLTest;
import one.pkg.mod.krypton_fnp.shared.ModSharedBootstrap;

@Mod("krypton_fnp")
public class NeoModBootstrap {
    public NeoModBootstrap() {
        FMLTest.test();
        ModSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}