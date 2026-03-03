package one.pkg.kfnp.mixin.network.pipeline.compression;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundCustomQueryPacket.class)
public class ClientboundCustomQueryPacketMixin {

    @Inject(method = "readPayload", at = @At("HEAD"), cancellable = true)
    private static void onReadPayload(Identifier id, FriendlyByteBuf buf, CallbackInfoReturnable<CustomQueryPayload> cir) {
        if (id.getNamespace().equals("krypton_fnp") && id.getPath().equals("compression_negotiation")) {
            int readableBytes = buf.readableBytes();
            if (readableBytes >= 0 && readableBytes <= 1048576) {
                byte[] data = new byte[readableBytes];
                buf.readBytes(data);
                
                cir.setReturnValue(new CustomQueryPayload() {
                    @Override
                    public @NonNull Identifier id() {
                        return id;
                    }

                    @Override
                    public void write(@NonNull FriendlyByteBuf b) {
                        b.writeBytes(data);
                    }

                    @Override
                    public String toString() {
                        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    }
                });
            }
        }
    }
}
