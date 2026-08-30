package one.pkg.kreno.mixin.network.chunk;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.culling.IKrenoTrackedEntity;
import one.pkg.kreno.shared.culling.ServerCullingManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.function.Predicate;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements IKrenoTrackedEntity {
    @Shadow
    @Final
    private Entity entity;
    
    @Shadow
    @Final
    private Set<ServerPlayerConnection> seenBy;
    
    @Shadow
    @Final
    private ServerEntity serverEntity;

    @Unique
    private int kreno$cullingTickCounter = 0;

    @Unique
    private boolean kreno$cullingActive = false;

    @Override
    public void kreno$checkCullingState() {
        if (!ServerCullingManager.shouldCullEntity(this.entity)) {
            if (this.kreno$cullingActive) {
                this.kreno$restoreTrackingState();
                this.kreno$cullingActive = false;
            }
            return;
        }
        this.kreno$cullingActive = true;
        this.kreno$cullingTickCounter++;
        if ((this.kreno$cullingTickCounter + this.entity.getId()) % 10 != 0) return;
        
        long currentTick = this.entity.level().getServer().getTickCount();
        for (ServerPlayerConnection conn : this.seenBy) {
            ServerPlayer player = conn.getPlayer();
            boolean isVisible = ServerCullingManager.isEntityVisible(player, this.entity, currentTick);
            boolean wasVisible = ServerCullingManager.getLastSentVisible(player, this.entity);

            if (isVisible != wasVisible) {
                if (isVisible) {
                    ServerCullingManager.setLastSentVisible(player, this.entity);
                    this.serverEntity.sendPairingData(player, conn::send);
                } else {
                    ServerCullingManager.setLastSentHidden(
                            player,
                            this.entity,
                            () -> this.serverEntity.sendPairingData(player, conn::send));
                    conn.send(new ClientboundRemoveEntitiesPacket(this.entity.getId()));
                }
            }
        }
    }

    @Unique
    private void kreno$restoreTrackingState() {
        for (ServerPlayerConnection conn : this.seenBy) {
            ServerPlayer player = conn.getPlayer();
            if (!ServerCullingManager.getLastSentVisible(player, this.entity)) {
                this.serverEntity.sendPairingData(player, conn::send);
            }
            ServerCullingManager.removePlayerEntityState(player, this.entity);
        }
    }

    @Redirect(method = "updatePlayer", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean kreno$onSeenByAdd(Set<ServerPlayerConnection> instance, Object e) {
        boolean added = instance.add((ServerPlayerConnection) e);
        if (added && ServerCullingManager.shouldCullEntity(this.entity)) {
            ServerCullingManager.setLastSentVisible(((ServerPlayerConnection) e).getPlayer(), this.entity);
        }
        return added;
    }

    @Redirect(method = "removePlayer", at = @At(value = "INVOKE", target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z"))
    private boolean kreno$onSeenByRemove(Set<ServerPlayerConnection> instance, Object e) {
        boolean removed = instance.remove((ServerPlayerConnection) e);
        if (removed) {
            ServerCullingManager.removePlayerEntityState(((ServerPlayerConnection) e).getPlayer(), this.entity);
        }
        return removed;
    }

    @Inject(method = "sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void kreno$sendToTrackingPlayers(Packet<?> packet, CallbackInfo ci) {
        boolean isCullingEnabled = ServerCullingManager.shouldCullEntity(this.entity);
        if (ModConfig.Mixin.isTrackedEntityOpt() || isCullingEnabled) {
            for (ServerPlayerConnection conn : this.seenBy) {
                if (!isCullingEnabled || ServerCullingManager.getLastSentVisible(conn.getPlayer(), this.entity)) {
                    conn.send(packet);
                }
            }
            ci.cancel();
        }
    }

    @Inject(method = "sendToTrackingPlayersFiltered", at = @At("HEAD"), cancellable = true)
    private void kreno$sendToTrackingPlayersFiltered(Packet<?> packet, Predicate<ServerPlayer> targetPredicate, CallbackInfo ci) {
        boolean isCullingEnabled = ServerCullingManager.shouldCullEntity(this.entity);
        if (ModConfig.Mixin.isTrackedEntityOpt() || isCullingEnabled) {
            for (ServerPlayerConnection conn : this.seenBy) {
                if (targetPredicate.test(conn.getPlayer())) {
                    if (!isCullingEnabled || ServerCullingManager.getLastSentVisible(conn.getPlayer(), this.entity)) {
                        conn.send(packet);
                    }
                }
            }
            ci.cancel();
        }
    }
}
