package one.pkg.kreno;

import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import one.pkg.kreno.shared.ModSharedBootstrap;
import one.pkg.kreno.shared.gui.KRenoConfigGUI;
import one.pkg.libsl.api.event.client.lifecycle.ClientLifecycleEvents;
import one.pkg.libsl.api.loader.JavaLoader;
import one.pkg.libsl.api.ui.oreui.OreUIDialog;
import one.pkg.libsl.api.ui.oreui.OreUIExampleScreen;
import one.pkg.loader.FMLTest;

@Mod("kreno")
public class NeoModBootstrap {
    private static final org.slf4j.Logger logg = org.slf4j.LoggerFactory.getLogger("Kreno");

    public NeoModBootstrap(IEventBus bus, ModContainer container) {
        FMLTest.test();
        ModSharedBootstrap.run();

        if (JavaLoader.INSTANCE.isClient()) {
            Client.init(container);

            // Only Test
            ClientLifecycleEvents.CLIENT_STARTED.register((client) -> {
                var screen = client.screen;
                client.execute(() ->
                        client.setScreen(new OreUIDialog(Component.literal("test"), screen)
                                .content(
                                        Component.literal("test").append(
                                                Component.literal("\ntest").withColor(CommonColors.YELLOW)
                                        ).append(
                                                Component.literal("\ntest")
                                        )
                                )
                                .onConfirm(() -> {
                                    logg.info("Test dialog confirmed");
                                    client.setScreen(screen);
                                    client.setScreen(new OreUIExampleScreen(screen));
                                })
                                .confirmText(Component.literal("Open Test Screen"))
                        )
                );
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
