package one.pkg.kreno.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import one.pkg.kreno.shared.network.TrafficMonitor;
import one.pkg.kreno.shared.network.util.ClientMonitorUtils;
import one.pkg.kreno.shared.network.util.VarIntUtil;
import one.pkg.libsl.api.loader.JavaLoader;

import java.util.UUID;

public class MinecraftCompressEncoder extends MessageToByteEncoder<ByteBuf> {

    private final VelocityCompressor compressor;
    private Connection kreno$cachedConnection;
    private UUID kreno$cachedUuid;
    private String kreno$cachedName = "Unknown";
    private boolean kreno$isPlayerResolved;
    private int threshold;

    public MinecraftCompressEncoder(int threshold, VelocityCompressor compressor) {
        this.threshold = threshold;
        this.compressor = compressor;
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

            ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), compressor, msg);
            try {
                compressor.deflate(compatibleIn, out);
            } finally {
                compatibleIn.release();
            }
        }
        TrafficMonitor.compressionEnabled = true;
        if (!kreno$isPlayerResolved) {
            if (kreno$cachedConnection == null) {
                kreno$cachedConnection = ctx.pipeline().get(Connection.class);
            }
            if (kreno$cachedConnection != null) {
                PacketListener listener = kreno$cachedConnection.getPacketListener();
                if (listener instanceof ServerGamePacketListenerImpl s) {
                    ServerPlayer player = s.getPlayer();
                    if (player != null) {
                        kreno$cachedUuid = player.getUUID();
                        kreno$cachedName = player.getScoreboardName();
                        kreno$isPlayerResolved = true;
                    }
                } else if (JavaLoader.INSTANCE.isClient()) {
                    Player player = ClientMonitorUtils.onMonitor(listener);
                    if (player != null) {
                        kreno$cachedUuid = player.getUUID();
                        kreno$cachedName = player.getScoreboardName();
                        kreno$isPlayerResolved = true;
                    }
                }
            }
        }
        TrafficMonitor.onOutboundCompressed(kreno$cachedUuid, kreno$cachedName, out.readableBytes());
    }

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect) {
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
        return MoreByteBufUtils.preferredBuffer(ctx.alloc(), compressor, initialBufferSize);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        compressor.close();
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
