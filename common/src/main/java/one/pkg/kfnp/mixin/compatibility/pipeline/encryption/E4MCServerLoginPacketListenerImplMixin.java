package one.pkg.kfnp.mixin.compatibility.pipeline.encryption;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import one.pkg.kfnp.shared.network.ClientConnectionEncryptionExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;

@Mixin(value = ServerLoginPacketListenerImpl.class, priority = 1500)
public class E4MCServerLoginPacketListenerImplMixin {
    @Shadow
    @Final
    Connection connection;

    @TargetHandler(
            mixin = "link.e4mc.mixin.ServerLoginPacketListenerImplMixin",
            name = "getCipher"
    )
    @Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Crypt;getCipher(ILjava/security/Key;)Ljavax/crypto/Cipher;"))
    private Cipher redirectGetCipher(int pOpMode, Key pKey) throws GeneralSecurityException {
        // Hijack this portion of the cipher initialization and set up our own encryption handler.
        ((ClientConnectionEncryptionExtension) this.connection).setupEncryption((SecretKey) pKey);

        // Turn the operation into a no-op.
        return null;
    }

    @Redirect(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setEncryptionKey(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V"))
    public void onKey$ignoreMinecraftEncryptionPipelineInjection(Connection connection, Cipher pDecryptingCipher, Cipher pEncryptingCipher) {
        // Turn the operation into a no-op.
    }
}