package me.steinborn.krypton.mixin.shared.config;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.xstopho.resourceconfigapi.ConfigConstants;
import net.xstopho.resourceconfigapi.config.ModConfig;
import net.xstopho.resourceconfigapi.network.ConfigNetwork;
import net.xstopho.resourceconfigapi.network.payloads.ConfigSyncPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ConfigConstants.class, remap = false)
public class ConfigConstantsMixin {

    @Redirect(method = "syncConfigs", at = @At(value = "INVOKE", target = "Lnet/xstopho/resourceconfigapi/network/ConfigNetwork;sendToClient(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"))
    private static void no_krypton_fnp(ConfigNetwork instance,
                                       ServerPlayer player,
                                       CustomPacketPayload payload,
                                       @Local ResourceLocation location,
                                       @Local ModConfig config) {
        if (!location.toString().equals("krypton:common/krypton_fnp")) {
            instance.sendToClient(player, new ConfigSyncPayload(location.toString(), config.toJson().toString()));
        }
    }
}