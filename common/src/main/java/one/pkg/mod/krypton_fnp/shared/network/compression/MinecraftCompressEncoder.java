package one.pkg.mod.krypton_fnp.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.FriendlyByteBuf;
import one.pkg.mod.krypton_fnp.shared.ModConfig;

import static one.pkg.mod.krypton_fnp.shared.network.util.SystemInfo.IS_WINDOWS;

public class MinecraftCompressEncoder extends MessageToByteEncoder<ByteBuf> {

    private final VelocityCompressor compressor;
    private final VelocityCompressor jCompressor;
    private int threshold;

    public MinecraftCompressEncoder(int threshold, VelocityCompressor compressor, VelocityCompressor jCompressor) {
        this.threshold = threshold;
        this.compressor = compressor;
        this.jCompressor = jCompressor;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        FriendlyByteBuf wrappedBuf = new FriendlyByteBuf(out);
        int uncompressed = msg.readableBytes();
        if (uncompressed < threshold) {
            // Under the threshold, there is nothing to do.
            wrappedBuf.writeVarInt(0);
            out.writeBytes(msg);
        } else {
            wrappedBuf.writeVarInt(uncompressed);

            VelocityCompressor selectedCompressor = getSelectedCompressor(uncompressed);
            ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), selectedCompressor, msg);
            try {
                selectedCompressor.deflate(compatibleIn, out);
            } finally {
                compatibleIn.release();
            }
        }
    }

    private VelocityCompressor getSelectedCompressor(int dataSize) {
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
            VelocityCompressor targetCompressor;

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

            return MoreByteBufUtils.preferredBuffer(ctx.alloc(), targetCompressor, initialBufferSize);

        }

        // We allocate bytes to be compressed plus 1 byte. This covers two cases:
        //
        // - Compression
        //    According to https://github.com/ebiggers/libdeflate/blob/master/libdeflate.h#L103,
        //    if the data compresses well (and we do not have some pathological case) then the maximum
        //    size the compressed size will ever be is the input size minus one.
        // - Uncompressed
        //    This is fairly obvious - we will then have one more than the uncompressed size.
        int initialBufferSize = msg.readableBytes() + 1;
        return MoreByteBufUtils.preferredBuffer(ctx.alloc(), compressor, initialBufferSize);
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