package one.pkg.kreno.mixin.network.pipeline.monitor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import one.pkg.kreno.shared.network.TrafficMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(PacketDecoder.class)
public class PacketDecoderMixin {
    @Unique
    private int kreno$initialBytes;

    @Inject(method = "decode", at = @At("HEAD"))
    private void kreno$onDecodeHead(ChannelHandlerContext ctx, ByteBuf input, List<Object> out, CallbackInfo ci) {
        kreno$initialBytes = input.readableBytes();
    }

    @Inject(method = "decode", at = @At("TAIL"))
    private void kreno$onDecodeTail(ChannelHandlerContext ctx, ByteBuf input, List<Object> out, CallbackInfo ci) {
        if (!out.isEmpty()) {
            Object packet = out.getLast();
            int consumed = kreno$initialBytes - input.readableBytes();
            if (consumed > 0) {
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
                        LocalPlayer player = Minecraft.getInstance().player;
                        if (player != null) {
                            uuid = player.getUUID();
                            name = player.getScoreboardName();
                        }
                    }
                }
                TrafficMonitor.onInboundPacket(uuid, name, packet.getClass().getSimpleName(), consumed);
            }
        }
    }
}
