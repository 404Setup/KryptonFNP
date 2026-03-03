package one.pkg.kfnp.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ByteToMessageDecoder;
import one.pkg.kfnp.shared.ModConfig;
import one.pkg.kfnp.shared.network.util.VarIntUtil;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;
import static one.pkg.kfnp.shared.network.util.SystemInfo.IS_LINUX;

/**
 * Decompresses a Minecraft packet.
 */
public class MinecraftCompressDecoder extends ByteToMessageDecoder {

    private static final int VANILLA_MAXIMUM_UNCOMPRESSED_SIZE = 8 * 1024 * 1024; // 8MiB
    private static final int HARD_MAXIMUM_UNCOMPRESSED_SIZE = 128 * 1024 * 1024; // 128MiB

    private static final int UNCOMPRESSED_CAP =
            ModConfig.Compression.isPermitOversizedPackets()
                    ? HARD_MAXIMUM_UNCOMPRESSED_SIZE : VANILLA_MAXIMUM_UNCOMPRESSED_SIZE;

    private final KFNPCompressor compressor;
    private final KFNPCompressor jCompressor;
    private final boolean validate;
    private int threshold;
    private int[] byteFreq;


    public MinecraftCompressDecoder(int threshold, boolean validate, KFNPCompressor compressor, KFNPCompressor jCompressor) {
        this.threshold = threshold;
        this.compressor = compressor;
        this.jCompressor = jCompressor;
        this.validate = validate;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int claimedUncompressedSize = VarIntUtil.readVarInt(in);

        if (claimedUncompressedSize == 0) {
            int actualUncompressedSize = in.readableBytes();
            checkState(actualUncompressedSize < threshold, "Actual uncompressed size %s is greater than"
                    + " threshold %s", actualUncompressedSize, threshold);
            out.add(in.retain());
            return;
        }

        if (claimedUncompressedSize > HARD_MAXIMUM_UNCOMPRESSED_SIZE) {
            throw new DecoderException("Uncompressed size " + claimedUncompressedSize + " exceeds hard maximum size of " + HARD_MAXIMUM_UNCOMPRESSED_SIZE);
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

        double threshold = ModConfig.Compression.BlendingMode.getRepetitiveThreshold();
        int minRequiredFreq = (int) Math.ceil(sampleSize * threshold);

        if (sampleSize >= 8) {
            int firstByte = compressed.getUnsignedByte(readerIndex);
            int consecutiveCount = 1;

            for (int i = 1; i < Math.min(8, sampleSize); i++) {
                if (compressed.getUnsignedByte(readerIndex + i) == firstByte) {
                    consecutiveCount++;
                } else {
                    break;
                }
            }

            if (consecutiveCount >= minRequiredFreq || consecutiveCount >= sampleSize / 2) {
                return true;
            }
        }

        // Optimized Path: Boyer-Moore Voting Algorithm (valid for > 50%)
        if (minRequiredFreq > sampleSize / 2) {
            int candidate = -1;
            int count = 0;
            for (int i = 0; i < sampleSize; i++) {
                int b = compressed.getUnsignedByte(readerIndex + i);
                if (count == 0) {
                    candidate = b;
                    count = 1;
                } else if (b == candidate) {
                    count++;
                } else {
                    count--;
                }
            }

            if (count < 2 * minRequiredFreq - sampleSize) {
                return false;
            }

            int freq = 0;
            for (int i = 0; i < sampleSize; i++) {
                int b = compressed.getUnsignedByte(readerIndex + i);
                if (b == candidate) {
                    if (++freq >= minRequiredFreq) return true;
                }
            }

            return false;
        }

        if (this.byteFreq == null) {
            this.byteFreq = new int[256];
        } else {
            Arrays.fill(this.byteFreq, 0);
        }
        for (int i = 0; i < sampleSize; i++) {
            int b = compressed.getUnsignedByte(readerIndex + i);
            if (++this.byteFreq[b] >= minRequiredFreq) {
                return true;
            }
        }

        return false;
    }

    private void decompress(KFNPCompressor compressor, ChannelHandlerContext ctx, ByteBuf in, List<Object> out,
                            int claimedUncompressedSize) throws Exception {
        VelocityCompressor velocityCompressor = null;
        if (compressor instanceof DeflateCompressor) {
            velocityCompressor = ((DeflateCompressor) compressor).delegate();
        }

        ByteBuf compatibleIn;
        if (velocityCompressor != null) {
            compatibleIn = ensureCompatible(ctx.alloc(), velocityCompressor, in);
        } else {
            if (!in.isDirect() || in.nioBufferCount() != 1) {
                compatibleIn = ctx.alloc().directBuffer(in.readableBytes());
                compatibleIn.writeBytes(in, in.readerIndex(), in.readableBytes());
            } else {
                compatibleIn = in.retain();
            }
        }

        ByteBuf uncompressed = velocityCompressor != null ? preferredBuffer(ctx.alloc(), velocityCompressor, claimedUncompressedSize) : ctx.alloc().directBuffer(claimedUncompressedSize);

        try {
            int oldReaderIndex = compatibleIn.readerIndex();
            compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
            int consumed = compatibleIn.readerIndex() - oldReaderIndex;
            if (in != compatibleIn) {
                in.skipBytes(consumed);
            }
            if (in.isReadable()) {
                 // In Minecraft protocol, one compressed payload should represent exactly one packet.
                 // If there's more data, it might be a malformed packet or a protocol error.
                 // However, Netty's pipeline might have combined multiple packets in one ByteBuf 'in'.
                 // But for Minecraft compression, the VarInt (uncompressed size) only applies to the NEXT chunk of data.
            }
            out.add(uncompressed);
        } catch (Exception e) {
            uncompressed.release();
            throw e;
        } finally {
            compatibleIn.release();
        }
    }


    private boolean shouldUseJavaFallback(int claimedUncompressedSize, ByteBuf compressed) {
        if (jCompressor == null
                || !IS_LINUX
                || claimedUncompressedSize < ModConfig.Compression.BlendingMode.getLinuxFallbackMinSize())
            return false;

        return detectRepetitiveData(compressed);
    }

    @Override
    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        compressor.close();
        if (jCompressor != null) jCompressor.close();
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
