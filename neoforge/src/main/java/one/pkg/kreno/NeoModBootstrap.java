package one.pkg.kreno;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import one.pkg.kreno.shared.ModSharedBootstrap;
import one.pkg.kreno.shared.gui.KRenoSimpleConfigGUI;
import one.pkg.libsl.loader.JavaLoader;
import one.pkg.loader.FMLTest;

@Mod("kreno")
public class NeoModBootstrap {
    public NeoModBootstrap(IEventBus bus, ModContainer container) {
        FMLTest.test();
        ModSharedBootstrap.run();

        if (JavaLoader.INSTANCE.isClient()) Client.init(container);
    }

    private static class Client {
        private static void init(ModContainer container) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (_, parent) -> new KRenoSimpleConfigGUI(parent));
        }
    }
}
