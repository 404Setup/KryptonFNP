package one.pkg.kreno.mixin.network.fix;

import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import one.pkg.libsl.api.loader.JavaLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerStatusPacketListenerImpl.class)
public class ServerStatusPacketListenerImplMixin {
    // Purpur (https://github.com/PurpurMC/Purpur) - fix 'outdated server' showing in ping before server fully boots - do not respond to pings before we know the protocol version
    // By: William Blake Galbreath <blake.galbreath@gmail.com>
    // Licensed under: MIT (https://opensource.org/licenses/MIT)
    @Inject(
            method = "handleStatusRequest",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void kreno$outdatedServerFix(ServerboundStatusRequestPacket packet, CallbackInfo ci) {
        if (JavaLoader.INSTANCE.server() != null) {
            ServerStatus serverStatus = JavaLoader.INSTANCE.server().getStatus();
            if (serverStatus == null || serverStatus.version().isEmpty())
                ci.cancel();
        }
    }
}
