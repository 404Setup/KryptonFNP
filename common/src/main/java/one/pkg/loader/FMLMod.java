package one.pkg.loader;

import org.spongepowered.asm.util.VersionNumber;

public interface FMLMod {
    String version();

    default VersionNumber getVersionNumber() {
        return VersionNumber.parse(version());
    }
}