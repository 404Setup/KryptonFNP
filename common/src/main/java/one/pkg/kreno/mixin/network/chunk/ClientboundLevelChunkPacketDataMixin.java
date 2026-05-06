package one.pkg.kreno.mixin.network.chunk;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.culling.BlockCullingUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundLevelChunkPacketData.class)
public class ClientboundLevelChunkPacketDataMixin {

    @Inject(method = "calculateChunkSize", at = @At("HEAD"), cancellable = true)
    private static void kreno$calculateChunkSize(LevelChunk chunk, CallbackInfoReturnable<Integer> cir) {
        if (ModConfig.Culling.isChunkBlockCullingEnabled()) {
            cir.setReturnValue(BlockCullingUtil.calculateAndCacheChunkSize(chunk));
        }
    }

    @Inject(method = "extractChunkData", at = @At("HEAD"), cancellable = true)
    private static void kreno$extractChunkData(FriendlyByteBuf buffer, LevelChunk chunk, CallbackInfo ci) {
        if (ModConfig.Culling.isChunkBlockCullingEnabled() && BlockCullingUtil.hasCachedData()) {
            BlockCullingUtil.writeCachedData(buffer);
            ci.cancel();
        }
    }
}
