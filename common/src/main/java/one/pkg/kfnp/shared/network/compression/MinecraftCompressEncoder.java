package one.pkg.kfnp.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import one.pkg.kfnp.shared.ModConfig;
import one.pkg.kfnp.shared.network.util.VarIntUtil;

import static one.pkg.kfnp.shared.network.util.SystemInfo.IS_WINDOWS;

public class MinecraftCompressEncoder extends MessageToByteEncoder<ByteBuf> {

    private final KFNPCompressor compressor;
    private final KFNPCompressor jCompressor;
    private int threshold;

    public MinecraftCompressEncoder(int threshold, KFNPCompressor compressor, KFNPCompressor jCompressor) {
        this.threshold = threshold;
        this.compressor = compressor;
        this.jCompressor = jCompressor;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        int uncompressed = msg.readableBytes();
        if (uncompressed < threshold) {
            // Under the threshold, there is nothing to do.
            VarIntUtil.writeVarInt(out, 0);
            out.writeBytes(msg);
        } else {
            VarIntUtil.writeVarInt(out, uncompressed);

            KFNPCompressor selectedCompressor = getSelectedCompressor(uncompressed);
            VelocityCompressor velocityCompressor = null;
            if (selectedCompressor instanceof DeflateCompressor) {
                velocityCompressor = ((DeflateCompressor) selectedCompressor).delegate();
            }

            ByteBuf compatibleIn = velocityCompressor != null ? MoreByteBufUtils.ensureCompatible(ctx.alloc(), velocityCompressor, msg) : msg.retain();
            try {
                selectedCompressor.deflate(compatibleIn, out);
            } finally {
                compatibleIn.release();
            }
        }
    }

    private KFNPCompressor getSelectedCompressor(int dataSize) {
        return ModConfig.Compression.BlendingMode.isEnabled() || jCompressor == null || shouldUseNativeCompression(dataSize)
                ? compressor
                : jCompressor;
    }


    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
            throws Exception {
        if (ModConfig.Compression.BlendingMode.isEnabled()) {
            int readableBytes = msg.readableBytes();
            int initialBufferSize;
            KFNPCompressor targetCompressor;

            if (readableBytes < threshold) {
                targetCompressor = compressor;
                initialBufferSize = readableBytes + 5;
            } else {
                targetCompressor = getSelectedCompressor(readableBytes);

                if (readableBytes < 1024)
                    initialBufferSize = readableBytes + 64;
                else if (readableBytes < 8192)
                    initialBufferSize = Math.max((int) (readableBytes * 0.6), 256) + 128;
                else
                    initialBufferSize = Math.max((int) (readableBytes * 0.4), 512) + 256;
            }

            if (targetCompressor instanceof DeflateCompressor) {
                return MoreByteBufUtils.preferredBuffer(ctx.alloc(), ((DeflateCompressor) targetCompressor).delegate(), initialBufferSize);
            }
            return ctx.alloc().directBuffer(initialBufferSize);
        }

        // We allocate bytes to be compressed plus 64 bytes. This covers two cases:
        //
        // - Compression
        //    According to https://github.com/ebiggers/libdeflate/blob/master/libdeflate.h#L103,
        //    if the data compresses well (and we do not have some pathological case) then the maximum
        //    size the compressed size will ever be is the input size minus one.
        // - Uncompressed
        //    This is fairly obvious - we will then have one more than the uncompressed size.
        //
        // However, we also need to account for the VarInt header that precedes the compressed data.
        // A single byte margin is insufficient if the VarInt length is > 1 byte (which is true for packets > 127 bytes).
        // Adding 64 bytes provides a safe margin for the VarInt header and any potential compression overhead,
        // preventing expensive reallocations.
        int initialBufferSize = msg.readableBytes() + 64;
        if (compressor instanceof DeflateCompressor) {
            return MoreByteBufUtils.preferredBuffer(ctx.alloc(), ((DeflateCompressor) compressor).delegate(), initialBufferSize);
        }
        return ctx.alloc().directBuffer(initialBufferSize);
    }

    private boolean shouldUseNativeCompression(int dataSize) {
        if (IS_WINDOWS && dataSize < 1024) return false;

        if (dataSize >= 8192) return true;

        return !IS_WINDOWS || dataSize >= 2048;
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
