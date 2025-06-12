package me.steinborn.krypton.mod;

import me.steinborn.krypton.mod.shared.KryptonSharedBootstrap;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod("krypton")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        KryptonSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}