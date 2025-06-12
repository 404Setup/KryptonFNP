package me.steinborn.krypton.mod;

import me.steinborn.krypton.mod.shared.KryptonSharedBootstrap;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;

@Mod("krypton")
public class KryptonBootstrap {
    public KryptonBootstrap() {
        KryptonSharedBootstrap.run(FMLLoader.getDist().isClient());
    }
}