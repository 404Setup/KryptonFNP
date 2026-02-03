package one.pkg.kfnp.mixin.compatibility.pipeline.encryption;

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import link.e4mc.dialtone.DialtoneChannel;
import net.minecraft.network.Connection;
import one.pkg.kfnp.shared.misc.KryptonPipelineEvent;
import one.pkg.kfnp.shared.network.ClientConnectionEncryptionExtension;
import one.pkg.kfnp.shared.network.pipeline.MinecraftCipherDecoder;
import one.pkg.kfnp.shared.network.pipeline.MinecraftCipherEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

@Mixin(value = Connection.class, priority = 1500)
public class E4MCConnectionMixin implements ClientConnectionEncryptionExtension {
    @Shadow
    private boolean encrypted;
    @Shadow
    private Channel channel;

    @Override
    public void setupEncryption(SecretKey key) throws GeneralSecurityException {
        // e4mc
        if (channel instanceof DialtoneChannel) {
            encrypted = true;
            return;
        }

        if (!this.encrypted) {
            VelocityCipher decryption = Natives.cipher.get().forDecryption(key);
            VelocityCipher encryption = Natives.cipher.get().forEncryption(key);

            this.encrypted = true;
            this.channel.pipeline().addBefore("splitter", "decrypt", new MinecraftCipherDecoder(decryption));
            this.channel.pipeline().addBefore("prepender", "encrypt", new MinecraftCipherEncoder(encryption));

            this.channel.pipeline().fireUserEventTriggered(KryptonPipelineEvent.ENCRYPTION_ENABLED);
        }
    }
}