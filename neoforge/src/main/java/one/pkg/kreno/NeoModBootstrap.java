package one.pkg.kreno;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import one.pkg.kreno.shared.ModSharedBootstrap;
import one.pkg.kreno.shared.command.KrenoCommand;
import one.pkg.kreno.shared.gui.KRenoConfigGUI;
import one.pkg.libsl.api.event.command.CommandRegistrationCallback;
import one.pkg.libsl.api.loader.JavaLoader;
import one.pkg.loader.FMLTest;

@Mod("kreno")
public class NeoModBootstrap {
    private static final org.slf4j.Logger logg = org.slf4j.LoggerFactory.getLogger("Kreno");

    public NeoModBootstrap(IEventBus bus, ModContainer container) {
        FMLTest.test();
        ModSharedBootstrap.run();

        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> {
            KrenoCommand.register(dispatcher);
        });

        if (JavaLoader.INSTANCE.isClient()) {
            Client.init(container);
        }

        //ModList.get().getModContainerById("kreno").get().registerExtensionPoint();
    }

    private static class Client {
        private static void init(ModContainer container) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (_, parent) -> new KRenoConfigGUI(parent));

            CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> {
                KrenoCommand.Client.register(dispatcher);
            });
        }
    }
}
