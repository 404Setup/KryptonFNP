package one.pkg.kreno;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import one.pkg.kreno.shared.ModSharedBootstrap;
import one.pkg.kreno.shared.gui.KRenoSimpleConfigGUI;
import one.pkg.loader.FMLTest;
import one.pkg.loader.Loader;

@Mod("kreno")
public class ForgeModBootstrap {
    public ForgeModBootstrap() {
        FMLTest.test();
        ModSharedBootstrap.run();

        if (Loader.INSTANCE.isClient()) Client.init();
    }

    @OnlyIn(Dist.CLIENT)
    private static class Client {
        private static void init() {
            MinecraftForge.registerConfigScreen(KRenoSimpleConfigGUI::new);
        }
    }
}
