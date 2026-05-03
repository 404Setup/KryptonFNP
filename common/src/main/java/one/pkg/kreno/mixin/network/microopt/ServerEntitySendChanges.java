package one.pkg.kreno.mixin.network.microopt;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import one.pkg.kreno.mixin.accessor.ClientboundMoveEntityPacketAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerEntity.class)
public class ServerEntitySendChanges {
    @Shadow
    private boolean wasOnGround;

    @Shadow
    @Final
    private Entity entity;

    /**
     * Rotation-only updates are also skipped if they don't actually change the state.
     * <p>
     * If the rotation and on-ground status haven't changed, we return null.
     * This prevents the server from sending redundant 'Rot' packets when an entity
     * is effectively stationary and its orientation is unchanged.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(IBBZ)Lnet/minecraft/network/protocol/game/ClientboundMoveEntityPacket$Rot;"
            )
    )
    private ClientboundMoveEntityPacket.Rot kreno$cancelUselessRotPacket(
            int id, byte yRot, byte xRot,
            boolean onGround,
            Operation<ClientboundMoveEntityPacket.Rot> original
    ) {
        if (this.wasOnGround == this.entity.onGround() && yRot == 0 && xRot == 0) {
            return null;
        }
        return original.call(id, yRot, xRot, onGround);
    }

    /**
     * There's no point in sending a movement update if the entity hasn't actually moved.
     * <p>
     * If the displacement (xa, ya, za) is zero, we abort the packet creation by returning null.
     * This saves both CPU time on the server and bandwidth on the network by skipping useless zero-delta updates.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "NEW",
                    target = "(ISSSZ)Lnet/minecraft/network/protocol/game/ClientboundMoveEntityPacket$Pos;"
            )
    )
    private ClientboundMoveEntityPacket.Pos kreno$cancelUselessPosPacket(
            int id,
            short xa,
            short ya,
            short za,
            boolean onGround,
            Operation<ClientboundMoveEntityPacket.Pos> original
    ) {
        if (xa == 0 && ya == 0 && za == 0) {
            return null;
        }
        return original.call(id, xa, ya, za, onGround);
    }

    /**
     * If an entity only rotates but doesn't move, sending a full 'PosRot' packet is wasteful.
     * <p>
     * We catch the packet right before it's sent to tracking players and, if we detect zero displacement,
     * we swap it for a much smaller 'Rot' packet. This ensures the client still gets the rotation update
     * without the overhead of the redundant movement data.
     */
    @ModifyArg(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private Packet<?> kreno$downgradePosRotPacket(Packet<?> packet) {
        if (packet instanceof ClientboundMoveEntityPacket.PosRot posRot) {
            ClientboundMoveEntityPacketAccessor accessor = (ClientboundMoveEntityPacketAccessor) posRot;
            if (posRot.getXa() == 0 && posRot.getYa() == 0 && posRot.getZa() == 0) {
                return new ClientboundMoveEntityPacket.Rot(accessor.getEntityId(), accessor.kreno$getYRot(),
                        accessor.kreno$getXRot(), posRot.isOnGround());
            }
        }
        return packet;
    }
}
