package one.pkg.loader;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface FMLTest {
    // This is a deliberate check.
    @ApiStatus.Internal
    static void test() {
        try {
            Class.forName("org.bukkit.advancement.Advancement");
            throw new UnsupportedOperationException("KryptonReno does not support running in an environment that mixes Bukkit with FML.");
        } catch (ClassNotFoundException ignored) {
        }
    }
}
