package one.pkg.kreno.mixin.network.pipeline.encryption;

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import one.pkg.kreno.shared.misc.KRenoPipelineEvent;
import one.pkg.kreno.shared.network.ClientConnectionEncryptionExtension;
import one.pkg.kreno.shared.network.pipeline.MinecraftCipherDecoder;
import one.pkg.kreno.shared.network.pipeline.MinecraftCipherEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

@Mixin(Connection.class)
public class ConnectionMixin implements ClientConnectionEncryptionExtension {
    @Shadow
    private Channel channel;

    @Unique
    private boolean encrypted;

    @Inject(
            method = "setEncryptionKey",
            at = @At("HEAD")
    )
    private void addEncrypted(Cipher decryptCipher, Cipher encryptCipher, CallbackInfo ci) {
        this.encrypted = true;
    }

    @Override
    public void setupEncryption(SecretKey key) throws GeneralSecurityException {
        if (!this.encrypted) {
            VelocityCipher decryption = Natives.cipher.get().forDecryption(key);
            VelocityCipher encryption = Natives.cipher.get().forEncryption(key);

            this.encrypted = true;
            this.channel.pipeline().addBefore("splitter", "decrypt", new MinecraftCipherDecoder(decryption));
            this.channel.pipeline().addBefore("prepender", "encrypt", new MinecraftCipherEncoder(encryption));

            this.channel.pipeline().fireUserEventTriggered(KRenoPipelineEvent.ENCRYPTION_ENABLED);
        }
    }
}
