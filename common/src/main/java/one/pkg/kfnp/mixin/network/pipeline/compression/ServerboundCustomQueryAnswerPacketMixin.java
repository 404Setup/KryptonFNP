package one.pkg.kfnp.mixin.network.pipeline.compression;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomQueryAnswerPacket.class)
public class ServerboundCustomQueryAnswerPacketMixin {

    @Inject(method = "readPayload", at = @At("HEAD"), cancellable = true)
    private static void onReadPayload(int transactionId, FriendlyByteBuf buf, CallbackInfoReturnable<CustomQueryAnswerPayload> cir) {
        // We don't have the ID here easily without more complex mixins,
        // but since this is head, we can't easily filter by ID unless we know the context.
        // However, we know that if it's our transaction ID, we want to capture the bytes.
        // The transaction ID for compression is 0x4B464E50.
        if (transactionId == 0x4B464E50) {
            int readableBytes = buf.readableBytes();
            if (readableBytes >= 0 && readableBytes <= 1048576) {
                byte[] data = new byte[readableBytes];
                buf.readBytes(data);
                
                cir.setReturnValue(b -> b.writeBytes(data));
            }
        }
    }
}
