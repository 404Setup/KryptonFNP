package me.steinborn.krypton.mixin.shared.network.pipeline.encryption;

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import me.steinborn.krypton.mod.shared.misc.KryptonPipelineEvent;
import me.steinborn.krypton.mod.shared.network.ClientConnectionEncryptionExtension;
import me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherDecoder;
import me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherEncoder;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

@Mixin(Connection.class)
public class ConnectionMixin implements ClientConnectionEncryptionExtension {
    @Shadow private boolean encrypted;
    @Shadow private Channel channel;

    @Override
    public void setupEncryption(SecretKey key) throws GeneralSecurityException {
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
