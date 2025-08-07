package one.pkg.mod.krypton_fnp;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import one.pkg.loader.FMLTest;
import one.pkg.mod.krypton_fnp.shared.ModSharedBootstrap;

@Mod("krypton_fnp")
public class ForgeModBootstrap {
    public ForgeModBootstrap() {
        FMLTest.test();
        ModSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}
