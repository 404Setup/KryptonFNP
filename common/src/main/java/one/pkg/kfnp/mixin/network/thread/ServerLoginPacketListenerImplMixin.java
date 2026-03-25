package one.pkg.kfnp.mixin.network.thread;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.notifications.ServerActivityMonitor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = ServerLoginPacketListenerImpl.class, priority = 900)
public abstract class ServerLoginPacketListenerImplMixin {
    @Final
    @Shadow
    private static Logger LOGGER;
    @Final
    @Shadow
    private static AtomicInteger UNIQUE_THREAD_ID;
    @Final
    @Shadow
    private Connection connection;
    @Final
    @Shadow
    private MinecraftServer server;
    @Shadow
    private String requestedUsername;
    @Shadow
    @Final
    private ServerActivityMonitor serverActivityMonitor;

    @Shadow
    protected abstract void startClientVerification(GameProfile profile);

    @Shadow
    public abstract void disconnect(Component component);

    @Inject(method = "handleKey",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setEncryptionKey(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V", shift = At.Shift.AFTER),
            cancellable = true)
    private void krypton_fnp$cacheAuthenticatorThread(ServerboundKeyPacket packet, CallbackInfo ci, @Local(name = "digest") String digest) {
        Runnable runnable = () -> {
            String name = Objects.requireNonNull(requestedUsername, "Player name not initialized");

            try {
                ProfileResult result = server.services().sessionService().hasJoinedServer(name, digest, krypton_fnp$getAddress());
                if (result != null) {
                    GameProfile profile = result.profile();
                    LOGGER.info("UUID of player {} is {}", profile.name(), profile.id());
                    serverActivityMonitor.reportLoginActivity();
                    startClientVerification(profile);
                } else if (server.isSingleplayer()) {
                    LOGGER.warn("Failed to verify username but will let them in anyway!");
                    startClientVerification(UUIDUtil.createOfflineProfile(name));
                } else {
                    disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                    LOGGER.error("Username '{}' tried to join with an invalid session", name);
                }
            } catch (AuthenticationUnavailableException authenticationunavailableexception) {
                if (server.isSingleplayer()) {
                    LOGGER.warn("Authentication servers are down but will let them in anyway!");
                    startClientVerification(UUIDUtil.createOfflineProfile(name));
                } else {
                    disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                    LOGGER.error("Couldn't verify username because servers are unavailable");
                }
            }
        };

        // for Java 21
        Thread.ofVirtual().name("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()).uncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).start(runnable);
        //krypton_fnp$authenticatorPool.execute(runnable);
        ci.cancel();
    }

    @Unique
    private InetAddress krypton_fnp$getAddress() {
        SocketAddress socketaddress = connection.getRemoteAddress();
        return server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress
                ? ((InetSocketAddress) socketaddress).getAddress()
                : null;
    }
}