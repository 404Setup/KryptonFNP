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

            ByteBuf compatibleIn;
            if (velocityCompressor != null) {
                compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), velocityCompressor, msg);
            } else {
                if (!msg.isDirect() || msg.nioBufferCount() != 1) {
                    compatibleIn = ctx.alloc().directBuffer(msg.readableBytes());
                    compatibleIn.writeBytes(msg, msg.readerIndex(), msg.readableBytes());
                } else {
                    compatibleIn = msg.retain();
                }
            }
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
            // For LZ4/Zstd, allocate a bit more to be safe
            return ctx.alloc().directBuffer(initialBufferSize + 128);
        }

        int readableBytes = msg.readableBytes();
        int initialBufferSize = readableBytes + 64;
        if (compressor instanceof DeflateCompressor) {
            return MoreByteBufUtils.preferredBuffer(ctx.alloc(), ((DeflateCompressor) compressor).delegate(), initialBufferSize);
        }
        // For LZ4/Zstd, we might need more space if data is incompressible
        return ctx.alloc().directBuffer(initialBufferSize + 128);
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
