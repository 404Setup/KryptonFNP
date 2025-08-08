package one.pkg.mod.krypton_fnp.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.network.FriendlyByteBuf;
import one.pkg.mod.krypton_fnp.shared.ModConfig;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;
import static one.pkg.mod.krypton_fnp.shared.network.util.SystemInfo.IS_LINUX;

/**
 * Decompresses a Minecraft packet.
 */
public class MinecraftCompressDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static final int VANILLA_MAXIMUM_UNCOMPRESSED_SIZE = 8 * 1024 * 1024; // 8MiB
    private static final int HARD_MAXIMUM_UNCOMPRESSED_SIZE = 128 * 1024 * 1024; // 128MiB

    private static final int UNCOMPRESSED_CAP =
            Boolean.getBoolean("krypton.permit-oversized-packets") || ModConfig.Compression.isPermitOversizedPackets()
                    ? HARD_MAXIMUM_UNCOMPRESSED_SIZE : VANILLA_MAXIMUM_UNCOMPRESSED_SIZE;

    private final VelocityCompressor compressor;
    private final VelocityCompressor jCompressor;
    private final boolean validate;
    private int threshold;


    public MinecraftCompressDecoder(int threshold, boolean validate, VelocityCompressor compressor, VelocityCompressor jCompressor) {
        this.threshold = threshold;
        this.compressor = compressor;
        this.jCompressor = jCompressor;
        this.validate = validate;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        FriendlyByteBuf bb = new FriendlyByteBuf(in);
        int claimedUncompressedSize = bb.readVarInt();

        if (claimedUncompressedSize == 0) {
            int actualUncompressedSize = in.readableBytes();
            checkState(actualUncompressedSize < threshold, "Actual uncompressed size %s is greater than"
                    + " threshold %s", actualUncompressedSize, threshold);
            out.add(in.retain());
            return;
        }

        if (validate) {
            checkState(claimedUncompressedSize >= threshold, "Uncompressed size %s is less than"
                    + " threshold %s", claimedUncompressedSize, threshold);
            checkState(claimedUncompressedSize <= UNCOMPRESSED_CAP,
                    "Uncompressed size %s exceeds hard threshold of %s", claimedUncompressedSize,
                    UNCOMPRESSED_CAP);
        }

        boolean v = shouldUseJavaFallback(claimedUncompressedSize, in);
        decompress(v ? jCompressor : compressor, ctx, in, out, claimedUncompressedSize);
    }

    private boolean detectRepetitiveData(ByteBuf compressed) {
        int readableBytes = compressed.readableBytes();
        if (readableBytes < 32) return false;

        int readerIndex = compressed.readerIndex();
        int sampleSize = Math.min(64, readableBytes);

        int[] byteFreq = new int[256];
        for (int i = 0; i < sampleSize; i++) {
            int b = compressed.getUnsignedByte(readerIndex + i);
            byteFreq[b]++;
        }

        int maxFreq = 0;
        for (int freq : byteFreq) maxFreq = Math.max(maxFreq, freq);

        double repetitiveRatio = (double) maxFreq / sampleSize;
        return repetitiveRatio >= ModConfig.Compression.BlendingMode.getRepetitiveThreshold();
    }

    private void decompress(VelocityCompressor compressor, ChannelHandlerContext ctx, ByteBuf in, List<Object> out,
                            int claimedUncompressedSize) throws Exception {
        ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), compressor, in);
        ByteBuf uncompressed = preferredBuffer(ctx.alloc(), compressor, claimedUncompressedSize);
        try {
            compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
            out.add(uncompressed);
        } catch (Exception e) {
            uncompressed.release();
            throw e;
        } finally {
            compatibleIn.release();
        }
    }


    private boolean shouldUseJavaFallback(int claimedUncompressedSize, ByteBuf compressed) {
        if (jCompressor == null || !IS_LINUX) return false;

        if (claimedUncompressedSize < ModConfig.Compression.BlendingMode.getLinuxFallbackMinSize())
            return false;

        return detectRepetitiveData(compressed);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        compressor.close();
        if (jCompressor != null) jCompressor.close();
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
