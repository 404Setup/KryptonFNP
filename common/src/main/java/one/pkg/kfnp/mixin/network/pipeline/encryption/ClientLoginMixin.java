package one.pkg.kfnp.mixin.network.pipeline.encryption;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import one.pkg.kfnp.shared.network.ClientConnectionEncryptionExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientLoginMixin {
    @Shadow
    @Final
    private Connection connection;
    @Unique
    private Key kfnp$secretKey;

    @Redirect(method = "handleHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Crypt;getCipher(ILjava/security/Key;)Ljavax/crypto/Cipher;"))
    private Cipher handleHello$initKey(int pOpMode, Key pKey) {
        if (this.kfnp$secretKey == null)
            this.kfnp$secretKey = pKey;
        return null;
    }

    @Inject(method = "setEncryption",
            at =
            @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientHandshakePacketListenerImpl;switchState(Lnet/minecraft/client/multiplayer/ClientHandshakePacketListenerImpl$State;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true)
    public void initEncryption(ServerboundKeyPacket keyPacket, Cipher decryptingCypher, Cipher encryptingCypher, CallbackInfo ci) {
        this.connection.send(keyPacket, PacketSendListener.thenRun(() -> {
            try {
                ((ClientConnectionEncryptionExtension) this.connection).setupEncryption((SecretKey) kfnp$secretKey);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }));
        ci.cancel();
    }
}