package me.steinborn.krypton.mod.nfmixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.steinborn.krypton.mod.shared.KryptonClientHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.negotiation.NegotiationResult;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(NetworkRegistry.class)
public class NetworkRegistryMixin {
    @Inject(method = "initializeOtherConnection(Lnet/minecraft/network/protocol/configuration/ClientConfigurationPacketListener;)V", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/network/negotiation/NegotiationResult;success()Z"), cancellable = true)
    private static void removeNeoForgeCheck(ClientConfigurationPacketListener listener, CallbackInfo ci, @Local NegotiationResult result) {
        var failed = result.failureReasons();
        if (failed.size() != 2) return;
        for (Map.Entry<ResourceLocation, Component> r : failed.entrySet()) {
            if (r.getKey().getNamespace().equals("resourceconfigapi")) {
                if (!result.success()) {
                    KryptonClientHelper.serverHasMod = false;
                    ci.cancel();
                } else {
                    KryptonClientHelper.serverHasMod = true;
                }
                return;
            }
        }
    }
}
