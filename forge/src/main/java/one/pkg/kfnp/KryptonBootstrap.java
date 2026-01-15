package one.pkg.kfnp;

import net.minecraftforge.fml.common.Mod;
import one.pkg.kfnp.shared.ModSharedBootstrap;

@Mod("krypton_fnp")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        ModSharedBootstrap.run();
    }
}
