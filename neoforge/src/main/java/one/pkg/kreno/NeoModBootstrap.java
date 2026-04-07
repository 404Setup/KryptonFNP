package one.pkg.kreno;

import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import one.pkg.kreno.shared.ModSharedBootstrap;
import one.pkg.kreno.shared.gui.KRenoConfigGUI;
import one.pkg.libsl.api.client.lifecycle.ClientLifecycleEvents;
import one.pkg.libsl.loader.JavaLoader;
import one.pkg.libsl.ui.oreui.OreUIDialog;
import one.pkg.loader.FMLTest;

@Mod("kreno")
public class NeoModBootstrap {
    public NeoModBootstrap(IEventBus bus, ModContainer container) {
        FMLTest.test();
        ModSharedBootstrap.run();

        if (JavaLoader.INSTANCE.isClient()) {
            Client.init(container);

            // Only Test
            ClientLifecycleEvents.CLIENT_STARTED.register((client) -> {
                client.execute(() -> {
                    client.setScreen(new OreUIDialog(Component.literal("test"), client.screen));
                });
            });
        }

        //ModList.get().getModContainerById("kreno").get().registerExtensionPoint();
    }

    private static class Client {
        private static void init(ModContainer container) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (_, parent) -> new KRenoConfigGUI(parent));
        }
    }
}
