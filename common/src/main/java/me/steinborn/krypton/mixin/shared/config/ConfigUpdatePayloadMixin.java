package me.steinborn.krypton.mixin.shared.config;

import net.minecraft.server.MinecraftServer;
import net.xstopho.resourceconfigapi.network.payloads.ConfigUpdatePayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ConfigUpdatePayload.class, remap = false)
public class ConfigUpdatePayloadMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void no_krypton_fnp(ConfigUpdatePayload payload, MinecraftServer server, CallbackInfo ci) {
        if (payload.file().equals("krypton:common/krypton_fnp")) {
            ci.cancel();
        }
    }
}
