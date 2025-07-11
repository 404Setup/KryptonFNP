package me.steinborn.krypton.mixin.shared.config;

import me.steinborn.krypton.mod.shared.KryptonClientHelper;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImplMixin {
    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    public void send(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundCustomPayloadPacket(
                CustomPacketPayload payload
        )) {
            if (!KryptonClientHelper.serverHasMod && payload.type().id().getNamespace().equals("resourceconfigapi")) {
                ci.cancel();
            }
        }
    }
}
