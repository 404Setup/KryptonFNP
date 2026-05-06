package one.pkg.kreno.mixin.network.chunk;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import one.pkg.kreno.shared.culling.ServerCullingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityCullingMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void kreno$onEntityRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        ServerCullingManager.removeEntity(self);
        if (self instanceof ServerPlayer player) {
            ServerCullingManager.removePlayer(player);
        }
    }
}
