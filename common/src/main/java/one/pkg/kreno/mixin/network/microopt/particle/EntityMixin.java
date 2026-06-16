package one.pkg.kreno.mixin.network.microopt.particle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import one.pkg.kreno.shared.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(
            method = "sendBubbleColumnParticles",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void kreno$lessPacket2(Level level, BlockPos pos, CallbackInfo ci) {
        if (ModConfig.Mixin.isParticlePacketOpt()) {
            ci.cancel();
        }
    }

    @Shadow
    public abstract void gameEvent(Holder<GameEvent> event);

    @Inject(
            method = "doWaterSplashEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;floor(D)I",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void kreno$lessPacket(CallbackInfo ci) {
        if (ModConfig.Mixin.isParticlePacketOpt()) {
            this.gameEvent(GameEvent.SPLASH);
            ci.cancel();
        }
    }
}
