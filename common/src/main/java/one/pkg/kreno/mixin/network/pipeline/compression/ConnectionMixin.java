package one.pkg.kreno.mixin.network.pipeline.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.Connection;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.misc.KRenoPipelineEvent;
import one.pkg.kreno.shared.network.compression.MinecraftCompressDecoder;
import one.pkg.kreno.shared.network.compression.MinecraftCompressEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Shadow
    private Channel channel;

    @Inject(method = "setupCompression", at = @At("HEAD"), cancellable = true)
    public void setCompressionThreshold(int threshold, boolean validateDecompressed, CallbackInfo ci) {
        Object decompressor = this.channel.pipeline().get("decompress");
        Object compressor = this.channel.pipeline().get("compress");

        if (threshold < 0) {
            if (decompressor instanceof CompressionDecoder || decompressor instanceof MinecraftCompressDecoder) {
                this.channel.pipeline().remove("decompress");
            }
            if (compressor instanceof CompressionEncoder || compressor instanceof MinecraftCompressEncoder) {
                this.channel.pipeline().remove("compress");
            }

            this.channel.pipeline().fireUserEventTriggered(KRenoPipelineEvent.COMPRESSION_DISABLED);
        } else {
            boolean installed = false;
            boolean updated = false;
            VelocityCompressor newlyCreatedCompressor = null;

            if (decompressor instanceof MinecraftCompressDecoder decoder) {
                decoder.setThreshold(threshold, validateDecompressed);
                updated = true;
            } else if (decompressor instanceof CompressionDecoder decoder) {
                decoder.setThreshold(threshold, validateDecompressed);
                updated = true;
            } else if (decompressor == null) {
                newlyCreatedCompressor = Natives.compress.get().create(ModConfig.Compression.getLevel());
                MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(
                        threshold, validateDecompressed, newlyCreatedCompressor);
                if (channel.pipeline().get("decoder") != null) {
                    channel.pipeline().addBefore("decoder", "decompress", decoder);
                } else {
                    channel.pipeline().addFirst("decompress", decoder);
                }
                installed = true;
            }

            if (compressor instanceof MinecraftCompressEncoder encoder) {
                encoder.setThreshold(threshold);
                updated = true;
            } else if (compressor instanceof CompressionEncoder encoder) {
                encoder.setThreshold(threshold);
                updated = true;
            } else if (compressor == null) {
                VelocityCompressor nativeCompressor = newlyCreatedCompressor != null
                        ? newlyCreatedCompressor
                        : Natives.compress.get().create(ModConfig.Compression.getLevel());
                MinecraftCompressEncoder encoder = new MinecraftCompressEncoder(threshold, nativeCompressor);
                if (channel.pipeline().get("encoder") != null) {
                    channel.pipeline().addBefore("encoder", "compress", encoder);
                } else {
                    channel.pipeline().addLast("compress", encoder);
                }
                installed = true;
            }

            if (installed) {
                this.channel.pipeline().fireUserEventTriggered(KRenoPipelineEvent.COMPRESSION_ENABLED);
            } else if (updated) {
                this.channel.pipeline().fireUserEventTriggered(KRenoPipelineEvent.COMPRESSION_THRESHOLD_UPDATED);
            }
        }

        ci.cancel();
    }
}
