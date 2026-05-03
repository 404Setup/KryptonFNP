package one.pkg.kreno.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundMoveEntityPacket.class)
public interface ClientboundMoveEntityPacketAccessor {
    @Accessor
    int getEntityId();

    @Accessor("yRot")
    byte kreno$getYRot();

    @Accessor("xRot")
    byte kreno$getXRot();
}
