package one.pkg.kfnp.mixin.network.pipeline.compression;

import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import one.pkg.kfnp.shared.ModConfig;
import one.pkg.kfnp.shared.network.compression.ConnectionCompressorExtension;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {
    @Unique
    private static final int KRYPTON_COMPRESSION_QUERY_ID = 0x4B464E50;
    @Shadow
    @Final
    Connection connection;

    @Shadow
    @Final
    private MinecraftServer server;
    @Unique
    private int krypton_fnp$delayedThreshold = -1;
    @Unique
    private int krypton_fnp$waitTicks = 0;
    @Unique
    private boolean krypton_fnp$waitingForNegotiation = false;

    @Inject(method = "handleHello", at = @At("RETURN"))
    public void onHandleHello(ServerboundHelloPacket packet, CallbackInfo ci) {
        if (this.connection.isMemoryConnection()) {
            return;
        }

        krypton_fnp$waitingForNegotiation = true;

        String features = ModConfig.Compression.getCompressor();
        if (ModConfig.Compression.isSmartReplay()) {
            features += ",smartReplay";
        }

        byte[] algBytes = features.getBytes(StandardCharsets.UTF_8);

        CustomQueryPayload payload = new CustomQueryPayload() {
            @Override
            public @NonNull Identifier id() {
                return Identifier.fromNamespaceAndPath("krypton_fnp", "compression_negotiation");
            }

            @Override
            public void write(FriendlyByteBuf buf) {
                buf.writeByteArray(algBytes);
            }
        };

        this.connection.send(new ClientboundCustomQueryPacket(KRYPTON_COMPRESSION_QUERY_ID, payload));
    }

    @Inject(method = "setCompressionThreshold", at = @At("HEAD"), cancellable = true)
    public void onSetCompressionThreshold(int threshold, CallbackInfo ci) {
        if (krypton_fnp$waitingForNegotiation && ((ConnectionCompressorExtension) this.connection).kfnp$getCompressor() == null) {
            krypton_fnp$delayedThreshold = threshold;
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if (krypton_fnp$waitingForNegotiation) {
            if (((ConnectionCompressorExtension) this.connection).kfnp$getCompressor() != null || krypton_fnp$waitTicks >= 40) {
                krypton_fnp$waitingForNegotiation = false;
                if (krypton_fnp$delayedThreshold != -1) {
                    int t = krypton_fnp$delayedThreshold;
                    krypton_fnp$delayedThreshold = -1;
                    this.connection.setupCompression(t, true);
                }
            } else {
                krypton_fnp$waitTicks++;
            }
        }
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    public void onHandleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        if (packet.transactionId() == KRYPTON_COMPRESSION_QUERY_ID) {
            CustomQueryAnswerPayload payload = packet.payload();
            String selectedAlgorithm = "deflate";
            boolean peerSupportsSmartReplay = false;

            if (payload != null) {
                try {
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
                ((ConnectionCompressorExtension) this.connection).kfnp$setPeerSupportsSmartReplay(true);
            }

            krypton_fnp$waitingForNegotiation = false;
            ci.cancel();
        }
    }
}
