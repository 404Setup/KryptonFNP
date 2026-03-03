package one.pkg.kfnp.mixin.network.pipeline.compression;

import com.velocitypowered.natives.compression.JavaVelocityCompressor;
import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.channel.Channel;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.Connection;
import one.pkg.kfnp.shared.ModConfig;
import one.pkg.kfnp.shared.ModSharedBootstrap;
import one.pkg.kfnp.shared.misc.KryptonPipelineEvent;
import one.pkg.kfnp.shared.network.compression.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.SocketAddress;

@Mixin(Connection.class)
public class ConnectionMixin implements ConnectionCompressorExtension {
    @Shadow
    private Channel channel;

    @Shadow
    private SocketAddress address;
    @Unique
    private String kfnp$compressor = ModConfig.Compression.getCompressor();
    @Unique
    private boolean kfnp$peerSupportsSmartReplay = false;

    @Unique
    private static boolean krypton_fnp$isKryptonOrVanillaDecompressor(Object o) {
        return o instanceof CompressionEncoder || o instanceof MinecraftCompressDecoder;
    }

    @Unique
    private static boolean krypton_fnp$isKryptonOrVanillaCompressor(Object o) {
        return o instanceof CompressionDecoder || o instanceof MinecraftCompressEncoder;
    }

    @Override
    public void kfnp$setCompressor(String compressor) {
        this.kfnp$compressor = compressor;
    }

    @Override
    public String kfnp$getCompressor() {
        return this.kfnp$compressor;
    }

    @Override
    public void kfnp$setPeerSupportsSmartReplay(boolean supports) {
        this.kfnp$peerSupportsSmartReplay = supports;
    }

    @Override
    public boolean kfnp$peerSupportsSmartReplay() {
        return this.kfnp$peerSupportsSmartReplay;
    }

    @Inject(method = "setupCompression", at = @At("HEAD"), cancellable = true)
    public void setCompressionThreshold(int threshold, boolean validateDecompressed, CallbackInfo ci) {
        if (threshold < 0) {
            if (krypton_fnp$isKryptonOrVanillaDecompressor(this.channel.pipeline().get("decompress"))) {
                this.channel.pipeline().remove("decompress");
            }
            if (krypton_fnp$isKryptonOrVanillaCompressor(this.channel.pipeline().get("compress"))) {
                this.channel.pipeline().remove("compress");
            }

            this.channel.pipeline().fireUserEventTriggered(KryptonPipelineEvent.COMPRESSION_DISABLED);
        } else {
            MinecraftCompressDecoder decoder = (MinecraftCompressDecoder) channel.pipeline()
                    .get("decompress");
            MinecraftCompressEncoder encoder = (MinecraftCompressEncoder) channel.pipeline()
                    .get("compress");
            if (decoder != null && encoder != null) {
                decoder.setThreshold(threshold);
                encoder.setThreshold(threshold);

                this.channel.pipeline().fireUserEventTriggered(KryptonPipelineEvent.COMPRESSION_THRESHOLD_UPDATED);
            } else {
                String requestedCompressor = this.kfnp$compressor != null ? this.kfnp$compressor : ModConfig.Compression.getCompressor();
                ModSharedBootstrap.LOGGER.info("Player {} negotiates in {} compression mode, SmartReplay status: {}",
                        this.address, requestedCompressor, kfnp$peerSupportsSmartReplay);

                KFNPCompressor compressor = KFNPCompressorFactory.create(requestedCompressor, ModConfig.Compression.getLevel());

                KFNPCompressor jCompressor = null;
                if (compressor instanceof DeflateCompressor deflateCompressor) {
                    VelocityCompressor vCompressor = deflateCompressor.delegate();
                    if (ModConfig.Compression.BlendingMode.isEnabled() || !(vCompressor instanceof JavaVelocityCompressor)) {
                        jCompressor = new DeflateCompressor(JavaVelocityCompressor.FACTORY.create(ModConfig.Compression.getLevel()));
                    }
                }

                encoder = new MinecraftCompressEncoder(threshold, compressor, jCompressor);
                decoder = new MinecraftCompressDecoder(threshold, validateDecompressed, compressor, jCompressor);

                channel.pipeline().addBefore("decoder", "decompress", decoder);
                channel.pipeline().addBefore("encoder", "compress", encoder);

                if (ModConfig.Compression.isSmartReplay() && this.kfnp$peerSupportsSmartReplay) {
                    channel.pipeline().addBefore("compress", "smart_replay_encoder", new one.pkg.kfnp.shared.network.compression.SmartReplayEncoder());
                    channel.pipeline().addAfter("decompress", "smart_replay_decoder", new one.pkg.kfnp.shared.network.compression.SmartReplayDecoder());
                }

                this.channel.pipeline().fireUserEventTriggered(KryptonPipelineEvent.COMPRESSION_ENABLED);
            }
        }

        ci.cancel();
    }
}
