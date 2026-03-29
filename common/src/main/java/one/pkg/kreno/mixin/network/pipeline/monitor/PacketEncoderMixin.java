package one.pkg.kreno.mixin.network.pipeline.monitor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import one.pkg.kreno.shared.network.TrafficMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PacketEncoder.class)
public class PacketEncoderMixin {
    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At("TAIL"))
    private void kreno$onEncode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf output, CallbackInfo ci) {
        Connection connection = ctx.pipeline().get(Connection.class);
        UUID uuid = null;
        String name = "Global";

        if (connection != null) {
            PacketListener listener = connection.getPacketListener();
            if (listener instanceof ServerGamePacketListenerImpl s) {
                ServerPlayer player = s.getPlayer();
                if (player != null) {
                    uuid = player.getUUID();
                    name = player.getScoreboardName();
                }
            } else if (listener instanceof ClientPacketListener c) {
                // On client side, it's the local player
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    uuid = player.getUUID();
                    name = player.getScoreboardName();
                }
            }
        }
        TrafficMonitor.onOutboundPacket(uuid, name, packet.getClass().getSimpleName(), output.readableBytes());
    }
}
