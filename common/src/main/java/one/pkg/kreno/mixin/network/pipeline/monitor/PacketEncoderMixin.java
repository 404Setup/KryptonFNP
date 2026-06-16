package one.pkg.kreno.mixin.network.pipeline.monitor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import one.pkg.kreno.shared.network.TrafficMonitor;
import one.pkg.kreno.shared.network.util.ClientMonitorUtils;
import one.pkg.kreno.shared.network.util.PacketNameCache;
import one.pkg.libsl.api.loader.JavaLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PacketEncoder.class)
public class PacketEncoderMixin {
    @Unique
    private Connection kreno$cachedConnection;
    @Unique
    private UUID kreno$cachedUuid;
    @Unique
    private String kreno$cachedName = "Global";
    @Unique
    private boolean kreno$isPlayerResolved;

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At("TAIL"))
    private void kreno$onEncode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf output, CallbackInfo ci) {
        if (!one.pkg.kreno.shared.ModConfig.Monitor.isEnabled()) return;
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
        TrafficMonitor.onOutboundPacket(kreno$cachedUuid, kreno$cachedName, PacketNameCache.getPacketName(packet), output.readableBytes());
    }
}
