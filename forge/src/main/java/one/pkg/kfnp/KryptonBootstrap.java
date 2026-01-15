package one.pkg.kfnp;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import one.pkg.kfnp.shared.ModSharedBootstrap;
import one.pkg.kfnp.shared.gui.KFNPConfigGUI;
import one.pkg.loader.Loader;

@Mod("krypton_fnp")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        ModSharedBootstrap.run();

        if (Loader.INSTANCE.isClient()) init();
    }

    @OnlyIn(Dist.CLIENT)
    private static void init() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new KFNPConfigGUI(parent))
        );
    }
}
