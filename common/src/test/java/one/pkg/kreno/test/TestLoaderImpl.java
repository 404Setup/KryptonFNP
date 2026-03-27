package one.pkg.kreno.test;

import one.pkg.loader.FMLMod;
import one.pkg.loader.LoaderImpl;
import org.jspecify.annotations.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestLoaderImpl implements LoaderImpl {
    @Override
    public Path getConfigPath() {
        Path p = Paths.get("build/test-config");
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            // ignore
        }
        return p;
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public boolean loaded(@NonNull String modid) {
        return false;
    }

    @Override
    public FMLMod mod(@NonNull String modid) {
        return null;
    }
}
