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
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.phys.Vec3;

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
     * Combines two optimizations into one safe call site wrapper:
     * <p>
     * 1. Guards against null packets: if a previous WrapOperation (kreno$cancelUselessRotPacket or
     * kreno$cancelUselessPosPacket) returned null to suppress a redundant update, we skip the send
     * entirely. Forwarding a null packet to sendToTrackingPlayers would eventually reach
     * ServerCommonPacketListenerImpl.send() and throw a NullPointerException.
     * <p>
     * 2. Downgrades PosRot to Rot when displacement is zero: if the entity only rotated without moving,
     * we swap the heavier PosRot packet for a smaller Rot packet, reducing bandwidth.
     */
    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private void kreno$sendChangesPacketGuard(ServerEntity.Synchronizer synchronizer, Packet<?> packet,
                                              Operation<Void> original) {
        if (packet == null) return;
        if (packet instanceof ClientboundMoveEntityPacket.PosRot posRot) {
            ClientboundMoveEntityPacketAccessor accessor = (ClientboundMoveEntityPacketAccessor) posRot;
            if (posRot.getXa() == 0 && posRot.getYa() == 0 && posRot.getZa() == 0) {
                packet = new ClientboundMoveEntityPacket.Rot(accessor.getEntityId(), accessor.kreno$getYRot(),
                        accessor.kreno$getXRot(), posRot.isOnGround());
            }
        }
        original.call(synchronizer, packet);
    }

    /**
     * Prevents the server from sending redundant 0-velocity packets.
     * When both the current movement and the last sent movement are small enough 
     * to be quantized as exactly 0 by LpVec3 (abs max < 3.051944088384301E-5),
     * we pretend the distance to the last movement is exactly 0.0.
     * This avoids waking up tracking clients with identical zero-velocity updates.
     */
    @Redirect(
            method = { "sendChanges", "handleMinecartPosRot" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
            )
    )
    private double kreno$optimizeRedundantMotion(Vec3 currentMovement, Vec3 lastSentMovement) {
        double diff = currentMovement.distanceToSqr(lastSentMovement);
        if (diff == 0.0) {
            return 0.0;
        }
        
        double maxCurr = Math.max(Math.abs(currentMovement.x), Math.max(Math.abs(currentMovement.y), Math.abs(currentMovement.z)));
        double maxLast = Math.max(Math.abs(lastSentMovement.x), Math.max(Math.abs(lastSentMovement.y), Math.abs(lastSentMovement.z)));
        
        if (maxCurr < 3.051944088384301E-5 && maxLast < 3.051944088384301E-5) {
            return 0.0;
        }
        
        return diff;
    }

}
