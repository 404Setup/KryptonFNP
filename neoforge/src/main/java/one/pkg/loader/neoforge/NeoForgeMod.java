package one.pkg.loader.neoforge;

import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import one.pkg.loader.FMLMod;


public class NeoForgeMod implements FMLMod {
    private final ModFileInfo info;

    public NeoForgeMod(ModFileInfo info) {
        this.info = info;
    }

    @Override
    public String version() {
        return info.versionString();
    }
}