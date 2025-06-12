package me.steinborn.krypton.mod;

import me.steinborn.krypton.mod.shared.KryptonSharedBootstrap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class KryptonBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        KryptonSharedBootstrap.run(FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT));
    }
}