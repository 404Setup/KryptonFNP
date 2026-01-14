package one.pkg.loader.neoforge;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import one.pkg.loader.LoaderImpl;

import java.nio.file.Path;

public class NeoForgeLoader implements LoaderImpl {
    @Override
    public Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isClient() {
        return FMLLoader.getCurrent().getDist().isClient();
    }
}
