package one.pkg.kfnp;

import net.minecraftforge.fml.common.Mod;
import one.pkg.kfnp.shared.ModSharedBootstrap;
import one.pkg.loader.FMLTest;

@Mod("krypton_fnp")
public class ForgeModBootstrap {
    public ForgeModBootstrap() {
        FMLTest.test();
        ModSharedBootstrap.run();
    }
}