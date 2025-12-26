package one.pkg.mod.krypton_fnp.mixin.network.pipeline.encryption;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import one.pkg.mod.krypton_fnp.shared.network.ClientConnectionEncryptionExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
    private Cipher handleHello$initKey(int ignored1, Key secretKey) {
        if (this.kfnp$secretKey == null)
            this.kfnp$secretKey = secretKey;
        return null;
    }

    @Redirect(method = "setEncryption", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketSendListener;thenRun(Ljava/lang/Runnable;)Lio/netty/channel/ChannelFutureListener;"))
    public ChannelFutureListener initEncryption(Runnable runnable) {
        return PacketSendListener.thenRun(() -> {
            try {
                ((ClientConnectionEncryptionExtension) this.connection).setupEncryption((SecretKey) kfnp$secretKey);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
