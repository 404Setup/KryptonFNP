package one.pkg.kreno.mixin.network.microopt;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.phys.Vec3;
import one.pkg.kreno.shared.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerEntity.class)
public class ServerEntitySendChanges {
    /**
     * Avoids a motion update only when the old and new velocities are both below the smallest
     * value represented by the client packet. Position and rotation synchronization is left
     * entirely to vanilla, including its periodic corrective packets.
     */
    @Redirect(
            method = {"sendChanges", "handleMinecartPosRot"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
            )
    )
    private double kreno$optimizeRedundantMotion(Vec3 currentMovement, Vec3 lastMovement) {
        double difference = currentMovement.distanceToSqr(lastMovement);
        if (!ModConfig.Mixin.isServerEntityMoveOpt() || difference == 0.0) {
            return difference;
        }

        double currentMaximum = Math.max(
                Math.abs(currentMovement.x), Math.max(Math.abs(currentMovement.y), Math.abs(currentMovement.z)));
        double lastMaximum = Math.max(
                Math.abs(lastMovement.x), Math.max(Math.abs(lastMovement.y), Math.abs(lastMovement.z)));

        if (currentMaximum < 3.051944088384301E-5 && lastMaximum < 3.051944088384301E-5) {
            return 0.0;
        }

        return difference;
    }
}
