package one.pkg.kreno.mixin.network.microopt.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.culling.ServerCullingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract void makePoofParticles();

    @Redirect(
            method = "tickEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;isEmpty()Z"
            )
    )
    private boolean kreno$lessPacket(List<ParticleOptions> instance) {
        boolean skip = ModConfig.Mixin.isParticlePacketOpt() && instance.isEmpty();
        if (skip) {
            ServerCullingManager.estimateParticlePacketOptSavings((LivingEntity)(Object)this, 15);
        }
        return skip;
    }

    @Redirect(
            method = "handleEntityEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;makePoofParticles()V"
            )
    )
    private void kreno$lessPacket2(LivingEntity instance) {
        if (ModConfig.Mixin.isParticlePacketOpt()) {
            ServerCullingManager.estimateParticlePacketOptSavings(instance, 15);
            return;
        }
        makePoofParticles();
    }
}
