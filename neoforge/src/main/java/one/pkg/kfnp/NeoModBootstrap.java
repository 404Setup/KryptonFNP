package one.pkg.kfnp;

import net.neoforged.fml.common.Mod;
import one.pkg.kfnp.shared.ModSharedBootstrap;
import one.pkg.loader.FMLTest;

@Mod("krypton_fnp")
public class NeoModBootstrap {
    public NeoModBootstrap() {
        FMLTest.test();
        ModSharedBootstrap.run();
    }
}