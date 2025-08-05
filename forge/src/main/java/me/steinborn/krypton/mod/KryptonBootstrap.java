package me.steinborn.krypton.mod;

import me.steinborn.krypton.mod.shared.KryptonFNPSharedBootstrap;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import one.pkg.loader.FMLTest;

@Mod("krypton_fnp")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        FMLTest.test();
        KryptonFNPSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}
