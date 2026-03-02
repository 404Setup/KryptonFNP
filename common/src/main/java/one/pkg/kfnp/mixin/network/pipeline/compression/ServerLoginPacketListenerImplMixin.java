package one.pkg.kfnp.mixin.network.pipeline.compression;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.resources.Identifier;
import one.pkg.kfnp.shared.ModConfig;
import one.pkg.kfnp.shared.network.compression.ConnectionCompressorExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {
    @Shadow @Final Connection connection;

    @Unique
    private static final int KRYPTON_COMPRESSION_QUERY_ID = 0x4B464E50;

    @Inject(method = "handleHello", at = @At("RETURN"))
    public void onHandleHello(net.minecraft.network.protocol.login.ServerboundHelloPacket packet, CallbackInfo ci) {
        String features = ModConfig.Compression.getCompressor();
        if (ModConfig.Compression.isSmartReplay()) {
            features += ",smartReplay";
        }

        byte[] algBytes = features.getBytes(StandardCharsets.UTF_8);

        CustomQueryPayload payload = new CustomQueryPayload() {
            @Override
            public Identifier id() {
                return Identifier.fromNamespaceAndPath("krypton_fnp", "compression_negotiation");
            }

            @Override
            public void write(FriendlyByteBuf buf) {
                buf.writeByteArray(algBytes);
            }
        };

        this.connection.send(new ClientboundCustomQueryPacket(KRYPTON_COMPRESSION_QUERY_ID, payload));
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    public void onHandleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        if (packet.transactionId() == KRYPTON_COMPRESSION_QUERY_ID) {
            CustomQueryAnswerPayload payload = packet.payload();
            String selectedAlgorithm = "deflate";
            boolean peerSupportsSmartReplay = false;

            // To be compatible with vanilla clients that drop unknown payloads,
            // we have to check if we can read the raw bytes. Since vanilla just drops the data,
            // the payload will be empty or null.
            if (payload != null) {
                try {
                    // Temporarily serialize it back to read the custom string if it's the generic Discarded type.
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    payload.write(buf);
                    if (buf.isReadable()) {
                        byte[] data = buf.readByteArray();
                        String response = new String(data, StandardCharsets.UTF_8);

                        String[] parts = response.split(",");
                        if (parts.length > 0 && !parts[0].isEmpty()) {
                            selectedAlgorithm = parts[0];
                        }
                        if (response.contains("smartReplay")) {
                            peerSupportsSmartReplay = true;
                        }
                    }
                    buf.release();
                } catch (Exception e) {
                    // Fallback on error
                }
            }

            ((ConnectionCompressorExtension) this.connection).kfnp$setCompressor(selectedAlgorithm);
            if (peerSupportsSmartReplay) {
                 // Tell connection it can use smart replay
                ((ConnectionCompressorExtension) this.connection).kfnp$setPeerSupportsSmartReplay(true);
            }

            ci.cancel();
        }
    }
}
