package one.pkg.kreno.mixin.network.chunk;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.culling.ServerCullingManager;
import one.pkg.kreno.shared.network.TrafficMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(
            method = "sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kreno$cullParticles(
            ServerPlayer player,
            boolean overrideLimiter,
            double x,
            double y,
            double z,
            Packet<?> packet,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!ModConfig.Culling.isParticleEnabled())
            return;

        Vec3 eyePos = player.getEyePosition();
        double dx = x - eyePos.x;
        double dy = y - eyePos.y;
        double dz = z - eyePos.z;
        double distanceSq = dx * dx + dy * dy + dz * dz;

        if (distanceSq < ServerCullingManager.NEAR_DISTANCE_SQ)
            return;

        Vec3 lookVec = player.getLookAngle();
        double dot = lookVec.x * dx + lookVec.y * dy + lookVec.z * dz;
        boolean inFOV;
        if (dot >= 0) {
            inFOV = true;
        } else {
            inFOV = (dot * dot <= 0.0225 * distanceSq);
        }

        Vec3 particlePos = new Vec3(x, y, z);
        if (!inFOV || !ServerCullingManager.isLineOfSightClear(player.level(), eyePos, particlePos)) {
            TrafficMonitor.onDroppedPacket(player.getUUID(), "particleCulling", 30);
            cir.setReturnValue(false);
        }
    }
}