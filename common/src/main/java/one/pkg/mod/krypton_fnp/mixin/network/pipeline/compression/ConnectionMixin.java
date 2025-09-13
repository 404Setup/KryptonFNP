package one.pkg.mod.krypton_fnp.mixin.network.pipeline.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import one.pkg.mod.krypton_fnp.shared.misc.KryptonPipelineEvent;
import one.pkg.mod.krypton_fnp.shared.network.compression.MinecraftCompressDecoder;
import one.pkg.mod.krypton_fnp.shared.network.compression.MinecraftCompressEncoder;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Shadow
    private Channel channel;

    @Unique
    private static boolean kryptonfnp$isKryptonOrVanillaDecompressor(Object o) {
        return o instanceof CompressionEncoder || o instanceof MinecraftCompressDecoder;
    }

    @Unique
    private static boolean kryptonfnp$isKryptonOrVanillaCompressor(Object o) {
        return o instanceof CompressionDecoder || o instanceof MinecraftCompressEncoder;
    }

    @Inject(method = "setupCompression", at = @At("HEAD"), cancellable = true)
    public void setCompressionThreshold(int compressionThreshold, boolean validate, CallbackInfo ci) {
        if (compressionThreshold < 0) {
            if (kryptonfnp$isKryptonOrVanillaDecompressor(this.channel.pipeline().get("decompress"))) {
                this.channel.pipeline().remove("decompress");
            }
            if (kryptonfnp$isKryptonOrVanillaCompressor(this.channel.pipeline().get("compress"))) {
                this.channel.pipeline().remove("compress");
            }

            this.channel.pipeline().fireUserEventTriggered(KryptonPipelineEvent.COMPRESSION_DISABLED);
        } else {
            MinecraftCompressDecoder decoder = (MinecraftCompressDecoder) channel.pipeline()
                    .get("decompress");
            MinecraftCompressEncoder encoder = (MinecraftCompressEncoder) channel.pipeline()
                    .get("compress");
            if (decoder != null && encoder != null) {
                decoder.setThreshold(compressionThreshold);
                encoder.setThreshold(compressionThreshold);

                this.channel.pipeline().fireUserEventTriggered(KryptonPipelineEvent.COMPRESSION_THRESHOLD_UPDATED);
            } else {
                VelocityCompressor compressor = Natives.compress.get().create(4);

                encoder = new MinecraftCompressEncoder(compressionThreshold, compressor);
                decoder = new MinecraftCompressDecoder(compressionThreshold, validate, compressor);

                channel.pipeline().addBefore("decoder", "decompress", decoder);
                channel.pipeline().addBefore("encoder", "compress", encoder);

                this.channel.pipeline().fireUserEventTriggered(KryptonPipelineEvent.COMPRESSION_ENABLED);
            }
        }

        ci.cancel();
    }
}
