package one.pkg.mod.krypton_fnp.mixin.network.experimental;

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
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
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
    static Logger LOGGER;
    @Final
    @Shadow
    private static AtomicInteger UNIQUE_THREAD_ID;
    //@Unique
    //private static final ExecutorService krypton_Multi$authenticatorPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("User Authenticator #%d").setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).build());
    @Final
    @Shadow
    Connection connection;
    @Final
    @Shadow
    MinecraftServer server;
    @Shadow
    String requestedUsername;

    @Invoker("startClientVerification")
    abstract void krypton_Multi$startClientVerification(GameProfile authenticatedProfile);

    @Invoker("disconnect")
    abstract void krypton_Multi$disconnect(Component reason);

    @Inject(method = "handleKey",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setEncryptionKey(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V", shift = At.Shift.AFTER),
            cancellable = true)
    private void krypton_Multi$cacheAuthenticatorThread(ServerboundKeyPacket packet, CallbackInfo ci, @Local String s) {
        Runnable runnable = () -> {
            String s1 = Objects.requireNonNull(requestedUsername, "Player name not initialized");

            try {
                ProfileResult profileresult = server.getSessionService().hasJoinedServer(s1, s, krypton_Multi$getAddress());
                if (profileresult != null) {
                    GameProfile gameprofile = profileresult.profile();
                    LOGGER.info("UUID of player {} is {}", gameprofile.getName(), gameprofile.getId());
                    krypton_Multi$startClientVerification(gameprofile);
                } else if (server.isSingleplayer()) {
                    LOGGER.warn("Failed to verify username but will let them in anyway!");
                    krypton_Multi$startClientVerification(UUIDUtil.createOfflineProfile(s1));
                } else {
                    krypton_Multi$disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                    LOGGER.error("Username '{}' tried to join with an invalid session", s1);
                }
            } catch (AuthenticationUnavailableException authenticationunavailableexception) {
                if (server.isSingleplayer()) {
                    LOGGER.warn("Authentication servers are down but will let them in anyway!");
                    krypton_Multi$startClientVerification(UUIDUtil.createOfflineProfile(s1));
                } else {
                    krypton_Multi$disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                    LOGGER.error("Couldn't verify username because servers are unavailable");
                }
            }
        };

        // for Java 21
        Thread.ofVirtual().name("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()).uncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).start(runnable);
        //krypton_Multi$authenticatorPool.execute(runnable);
        ci.cancel();
    }

    @Unique
    private InetAddress krypton_Multi$getAddress() {
        SocketAddress socketaddress = connection.getRemoteAddress();
        return server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress
                ? ((InetSocketAddress) socketaddress).getAddress()
                : null;
    }
}