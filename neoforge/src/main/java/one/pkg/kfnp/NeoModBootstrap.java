package one.pkg.kfnp;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import one.pkg.kfnp.shared.ModSharedBootstrap;
import one.pkg.kfnp.shared.gui.KFNPConfigGUI;
import one.pkg.loader.FMLTest;
import one.pkg.loader.Loader;

@Mod("krypton_fnp")
public class NeoModBootstrap {
    public NeoModBootstrap(IEventBus bus, ModContainer container) {
        FMLTest.test();
        ModSharedBootstrap.run();

        if (Loader.INSTANCE.isClient()) Client.init(container);
    }

    @OnlyIn(Dist.CLIENT)
    private static class Client {
        private static void init(ModContainer container) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> new KFNPConfigGUI(parent));
        }
    }
}