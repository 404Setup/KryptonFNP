package one.pkg.kreno.mixin.network.pipeline.encryption;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.util.HttpUtil;
import one.pkg.kreno.shared.network.ClientConnectionEncryptionExtension;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.function.Consumer;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientLoginMixin {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private Connection connection;
    @Shadow
    @Final
    private Consumer<Component> updateStatus;
    @Shadow
    @Final
    @Nullable
    private ServerData serverData;
    @Unique
    private Key kreno$secretKey;

    @Shadow
    private Component authenticateServer(String serverHash) {
        return null;
    }

    @Redirect(method = "handleHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Crypt;getCipher(ILjava/security/Key;)Ljavax/crypto/Cipher;"))
    private Cipher handleHello$initKey(int pOpMode, Key pKey) {
        if (this.kreno$secretKey == null)
            this.kreno$secretKey = pKey;
        return null;
    }

    @Inject(method = "handleHello", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    public void handleExec(ClientboundHelloPacket packet, CallbackInfo ci,
                           @Local(name = "s") String s, @Local(name = "serverboundkeypacket") ServerboundKeyPacket serverboundkeypacket) {
        HttpUtil.DOWNLOAD_EXECUTOR.submit(() -> {
            Component component = this.authenticateServer(s);
            if (component != null) {
                if (this.serverData == null || !this.serverData.isLan()) {
                    this.connection.disconnect(component);
                    return;
                }

                LOGGER.warn(component.getString());
            }

            this.updateStatus.accept(Component.translatable("connect.encrypting"));
            this.connection.send(serverboundkeypacket, PacketSendListener.thenRun(() -> {
                try {
                    ((ClientConnectionEncryptionExtension) this.connection).setupEncryption((SecretKey) kreno$secretKey);
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
            }));
        });

        ci.cancel();
    }
}