package one.pkg.loader.forge;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import one.pkg.loader.FMLMod;
import one.pkg.loader.LoaderImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class ForgeLoader implements LoaderImpl {

    @Override
    public Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    @Override
    public boolean loaded(@NotNull String modid) {
        return FMLLoader.getLoadingModList().getModFileById(modid) != null;
    }

    @Override
    public @Nullable FMLMod mod(@NotNull String modid) {
        var m = FMLLoader.getLoadingModList().getModFileById(modid);
        return m == null ? null : new ForgeMod(m);
    }
}
