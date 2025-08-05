package me.steinborn.krypton.mod;

import me.steinborn.krypton.mod.shared.KryptonSharedBootstrap;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import one.pkg.loader.FMLTest;

@Mod("krypton")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        FMLTest.test();
        KryptonSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}
