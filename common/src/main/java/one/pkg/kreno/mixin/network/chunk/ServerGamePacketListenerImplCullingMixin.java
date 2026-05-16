package one.pkg.kreno.mixin.network.chunk;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.state.BlockState;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.culling.ILightUpdatePacketDataSavedBytes;
import one.pkg.kreno.shared.culling.ServerCullingManager;
import one.pkg.kreno.shared.network.TrafficMonitor;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Consumer;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplCullingMixin extends ServerCommonPacketListenerImpl {
    @Shadow
    public ServerPlayer player;

    @Unique
    private static final ThreadLocal<Boolean> kreno$bypassCulling = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public ServerGamePacketListenerImplCullingMixin(MinecraftServer server, Connection connection, CommonListenerCookie cookie) {
        super(server, connection, cookie);
    }

    @Unique
    private void kreno$sendDirect(Packet<?> packet) {
        kreno$bypassCulling.set(Boolean.TRUE);
        try {
            super.send(packet);
        } finally {
            kreno$bypassCulling.set(Boolean.FALSE);
        }
    }

    @Override
    public void send(@NonNull Packet<?> packet) {
        if (kreno$bypassCulling.get()) {
            super.send(packet);
            return;
        }

        Consumer<Packet<?>> direct = this::kreno$sendDirect;
        ServerCullingManager.maybeProcessPendingRefreshes(this.player, direct);

        if (packet instanceof ClientboundBlockEntityDataPacket blockEntityPacket) {
            if (!ServerCullingManager.isBlockVisible(this.player, blockEntityPacket.getPos())) {
                if (ServerCullingManager.recordDroppedBlock(this.player, blockEntityPacket.getPos())) {
                    TrafficMonitor.onDroppedPacket(this.player.getUUID(), "blockEntityCulling", 20);
                    return;
                }
            }
        } else if (packet instanceof ClientboundBlockUpdatePacket blockUpdatePacket) {
            if (!ServerCullingManager.isBlockVisible(this.player, blockUpdatePacket.getPos())) {
                if (ServerCullingManager.recordDroppedBlock(this.player, blockUpdatePacket.getPos())) {
                    TrafficMonitor.onDroppedPacket(this.player.getUUID(), "blockCulling", 12);
                    return;
                }
                super.send(packet);
                return;
            }

            if (ModConfig.Culling.isChunkBlockCullingEnabled()) {
                BlockState newState = blockUpdatePacket.getBlockState();
                if (!newState.isSolidRender()) {
                    ServerCullingManager.refreshAdjacentBlocks(this.player, blockUpdatePacket.getPos(), direct);
                }
            }
        } else if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
            int saved = ((ILightUpdatePacketDataSavedBytes) chunkPacket.getLightData()).kreno$getSavedBytes();
            if (saved > 0) {
                TrafficMonitor.onDroppedPacket(this.player.getUUID(), "chunkLightCulling", saved);
            }
        } else if (packet instanceof ClientboundLightUpdatePacket lightPacket) {
            int saved = ((ILightUpdatePacketDataSavedBytes) lightPacket.getLightData()).kreno$getSavedBytes();
            if (saved > 0) {
                TrafficMonitor.onDroppedPacket(this.player.getUUID(), "chunkLightCulling", saved);
            }
        }
        super.send(packet);
    }
}
