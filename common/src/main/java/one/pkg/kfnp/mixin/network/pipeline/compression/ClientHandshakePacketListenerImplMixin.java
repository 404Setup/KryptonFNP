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
        if (payload instanceof DiscardedQueryPayload dqp) {
            var id = dqp.id();
            if (id.getNamespace().equals("krypton_fnp") && id.getPath().equals("compression_negotiation")) {

                String clientPreferred = ModConfig.Compression.getCompressor();
                boolean clientWantsSmartReplay = ModConfig.Compression.isSmartReplay();

                // In vanilla, DiscardedQueryPayload drops the data payload.
                // It is difficult to read what the server sent if Forge doesn't wrap it with something holding the buffer.
                // We default to our preferred algorithm.
                String selectedAlgorithm = clientPreferred;

                // We don't have the server's capabilities securely,
                // but if we are sending our preferences we can at least try to enable it.
                // To be safe, we disable smartReplay unless we have explicit proof.
                // Note: a robust implementation needs a proper PayloadCodec.
                boolean useSmartReplay = false;

                ((ConnectionCompressorExtension) this.connection).kfnp$setCompressor(selectedAlgorithm);

                String responseStr = selectedAlgorithm + (useSmartReplay ? ",smartReplay" : "");
                byte[] responseBytes = responseStr.getBytes(StandardCharsets.UTF_8);

                // Construct a custom answer payload for the response
                CustomQueryAnswerPayload responsePayload = buf -> buf.writeByteArray(responseBytes);

                this.connection.send(new ServerboundCustomQueryAnswerPacket(packet.transactionId(), responsePayload));
                ci.cancel();
            }
        }
    }
}
