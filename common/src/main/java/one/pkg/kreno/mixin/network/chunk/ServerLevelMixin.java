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
        if (!ModConfig.Culling.isEntityEnabled())
            return;

        Vec3 eyePos = player.getEyePosition();
        Vec3 particlePos = new Vec3(x, y, z);
        Vec3 toParticle = particlePos.subtract(eyePos).normalize();
        Vec3 lookVec = player.getLookAngle();
        double dot = lookVec.dot(toParticle);

        boolean inFOV = dot >= -0.15 || dot <= -0.5;
        if (!inFOV || !ServerCullingManager.isLineOfSightClear(player.level(), eyePos, particlePos)) {
            TrafficMonitor.onDroppedPacket(player.getUUID(), "particleCulling", 30);
            cir.setReturnValue(false);
        }
    }
}