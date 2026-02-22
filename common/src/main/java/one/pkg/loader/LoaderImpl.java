package one.pkg.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface LoaderImpl {
    Path getConfigPath();

    boolean isClient();

    boolean loaded(@NotNull String modid);

    @Nullable
    FMLMod mod(@NotNull String modid);
}