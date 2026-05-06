package one.pkg.kreno.mixin.network.chunk;

import net.minecraft.server.level.ServerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import one.pkg.kreno.shared.culling.IKrenoTrackedEntity;

@Mixin(ServerEntity.class)
public class ServerEntityCullingMixin {
    @Final
    @Shadow private ServerEntity.Synchronizer synchronizer;

    @Inject(method = "sendChanges", at = @At("HEAD"))
    private void kreno$pollCulling(CallbackInfo ci) {
        if (this.synchronizer instanceof IKrenoTrackedEntity tracked) {
            tracked.kreno$checkCullingState();
        }
    }
}
