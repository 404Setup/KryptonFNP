package one.pkg.kreno.mixin.network.chunk;

import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import one.pkg.kreno.shared.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.List;

/**
 * Reduces wire size of light data carried by
 * {@link net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket} and
 * {@link net.minecraft.network.protocol.game.ClientboundLightUpdatePacket}.
 * <p>
 * Vanilla {@link DataLayer#isEmpty()} only reports {@code true} when the backing array has not yet
 * been allocated; a section whose 2048-byte buffer was allocated but contains all zeros is still
 * sent as 2050 bytes (VarInt length + payload). On the client side, a section announced via the
 * empty mask is materialised as {@code new DataLayer()} which is value-equivalent to a fully zero
 * array, so promoting such sections to the empty mask is observably identical for vanilla clients.
 */
@Mixin(ClientboundLightUpdatePacketData.class)
public class ClientboundLightUpdatePacketDataMixin {

    @Unique
    private static boolean kreno$isAllZero(byte[] data) {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    @Inject(
            method = "prepareSectionData",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kreno$prepareSectionData(
            ChunkPos pos,
            LevelLightEngine lightEngine,
            LightLayer layer,
            int sectionIndex,
            BitSet mask,
            BitSet emptyMask,
            List<byte[]> updates,
            CallbackInfo ci
    ) {
        if (!ModConfig.Culling.isChunkLightCullingEnabled()) {
            return;
        }

        DataLayer data = lightEngine.getLayerListener(layer)
                .getDataLayerData(SectionPos.of(pos, lightEngine.getMinLightSection() + sectionIndex));
        if (data == null) {
            ci.cancel();
            return;
        }

        if (data.isEmpty()) {
            emptyMask.set(sectionIndex);
            ci.cancel();
            return;
        }

        byte[] raw = data.getData();
        if (kreno$isAllZero(raw)) {
            emptyMask.set(sectionIndex);
        } else {
            mask.set(sectionIndex);
            updates.add(raw.clone());
        }
        ci.cancel();
    }
}
