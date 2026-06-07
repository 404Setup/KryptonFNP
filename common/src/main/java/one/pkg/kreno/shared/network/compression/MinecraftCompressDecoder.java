package one.pkg.kreno.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.network.TrafficMonitor;
import one.pkg.kreno.shared.network.util.ClientMonitorUtils;
import one.pkg.kreno.shared.network.util.VarIntUtil;
import one.pkg.libsl.api.loader.JavaLoader;

import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkState;
import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;

/**
 * Decompresses a Minecraft packet.
 */
public class MinecraftCompressDecoder extends MessageToMessageDecoder<ByteBuf> {

    private Connection kreno$cachedConnection;
    private UUID kreno$cachedUuid;
    private String kreno$cachedName = "Unknown";
    private boolean kreno$isPlayerResolved;

    private static final int VANILLA_MAXIMUM_UNCOMPRESSED_SIZE = 8 * 1024 * 1024;
    private static final int HARD_MAXIMUM_UNCOMPRESSED_SIZE = 128 * 1024 * 1024;

    private static final int UNCOMPRESSED_CAP =
            ModConfig.Compression.isPermitOversizedPackets()
                    ? HARD_MAXIMUM_UNCOMPRESSED_SIZE : VANILLA_MAXIMUM_UNCOMPRESSED_SIZE;

    private final VelocityCompressor compressor;
    private final boolean validate;
    private int threshold;


    public MinecraftCompressDecoder(int threshold, boolean validate, VelocityCompressor compressor) {
        this.threshold = threshold;
        this.compressor = compressor;
        this.validate = validate;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
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
        TrafficMonitor.onInboundCompressed(kreno$cachedUuid, kreno$cachedName, in.readableBytes() + VarIntUtil.getVarIntLength(in.readableBytes()));
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

        decompress(compressor, ctx, in, out, claimedUncompressedSize);
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

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        compressor.close();
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
