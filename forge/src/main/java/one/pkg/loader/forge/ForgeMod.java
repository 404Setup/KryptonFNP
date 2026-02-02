package one.pkg.loader.forge;

import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import one.pkg.loader.FMLMod;

public class ForgeMod implements FMLMod {
    private final ModFileInfo info;

    public ForgeMod(ModFileInfo info) {
        this.info = info;
    }

    @Override
    public String version() {
        return info.versionString();
    }
}
