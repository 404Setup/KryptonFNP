package one.pkg.kreno.mixin.network.microopt.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {

    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private ServerEntity.Synchronizer synchronizer;

    @Shadow
    private Vec3 lastSentMovement;

    @WrapOperation(
            method = "sendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private void kreno$sendLessPacket(
            ServerEntity.Synchronizer instance,
            Packet<? super ClientGamePacketListener> packet,
            Operation<Void> original,
            @Local(name = "positionChanged") boolean positionChanged
    ) {
        if (packet instanceof ClientboundSetEntityMotionPacket) {
            if ((this.entity instanceof ItemEntity && positionChanged) ||
                    this.entity instanceof EyeOfEnder ||
                    this.entity instanceof Squid ||
                    this.entity instanceof ShulkerBullet) {
                original.call(instance, packet);
            }
        } else {
            original.call(instance, packet);
        }
    }

}
