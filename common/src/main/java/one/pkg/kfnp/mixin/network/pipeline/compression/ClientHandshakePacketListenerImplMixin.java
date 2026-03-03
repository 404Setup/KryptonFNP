package one.pkg.kfnp.mixin.network.pipeline.compression;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import net.minecraft.resources.Identifier;
import one.pkg.kfnp.shared.ModConfig;
import one.pkg.kfnp.shared.network.compression.ConnectionCompressorExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientHandshakePacketListenerImplMixin {
    @Shadow
    @Final
    private Connection connection;

    @Inject(method = "handleCustomQuery", at = @At("HEAD"), cancellable = true)
    public void onHandleCustomQuery(ClientboundCustomQueryPacket packet, CallbackInfo ci) {
        CustomQueryPayload payload = packet.payload();
        if (payload != null) {
            var id = payload.id();
            if (id.getNamespace().equals("krypton_fnp") && id.getPath().equals("compression_negotiation")) {

                String clientPreferred = ModConfig.Compression.getCompressor();
                boolean clientWantsSmartReplay = ModConfig.Compression.isSmartReplay();

                String serverRequested = null;
                boolean serverSupportsSmartReplay = false;

                try {
                    io.netty.buffer.ByteBuf tempBuf = io.netty.buffer.Unpooled.buffer();
                    net.minecraft.network.FriendlyByteBuf fbb = new net.minecraft.network.FriendlyByteBuf(tempBuf);
                    payload.write(fbb);
                    if (fbb.isReadable()) {
                        byte[] data = fbb.readByteArray();
                        String response = new String(data, StandardCharsets.UTF_8);
                        String[] parts = response.split(",");
                        if (parts.length > 0 && !parts[0].isEmpty()) {
                            serverRequested = parts[0];
                        }
                        if (response.contains("smartReplay")) {
                            serverSupportsSmartReplay = true;
                        }
                    }
                    fbb.release();
                } catch (Exception e) {
                    // Ignored
                }

                String selectedAlgorithm = serverRequested != null ? serverRequested : clientPreferred;
                boolean useSmartReplay = serverSupportsSmartReplay && clientWantsSmartReplay;

                one.pkg.kfnp.shared.ModSharedBootstrap.LOGGER.info("[KryptonFNP] Server requested: {}, Client preferred: {}. Selected: {}. SmartReplay: {}", 
                    serverRequested != null ? serverRequested : "None (Vanilla)", clientPreferred, selectedAlgorithm, useSmartReplay);

                ((ConnectionCompressorExtension) this.connection).kfnp$setCompressor(selectedAlgorithm);

                String responseStr = selectedAlgorithm + (useSmartReplay ? ",smartReplay" : "");
                byte[] responseBytes = responseStr.getBytes(StandardCharsets.UTF_8);

                CustomQueryAnswerPayload responsePayload = buf -> buf.writeByteArray(responseBytes);

                this.connection.send(new ServerboundCustomQueryAnswerPacket(packet.transactionId(), responsePayload));
                ci.cancel();
            }
        }
    }
}
