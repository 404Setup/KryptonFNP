package one.pkg.loader.forge;

import net.minecraftforge.fml.loading.FMLPaths;
import one.pkg.loader.LoaderImpl;

import java.nio.file.Path;

public class ForgeLoader implements LoaderImpl {

    @Override
    public Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }
}