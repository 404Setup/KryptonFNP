package one.pkg.kreno.mixin.network.quic.client;

import net.minecraft.client.multiplayer.resolver.ServerAddress;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.network.quic.ServerAddressProperties;
import one.pkg.libsl.api.loader.JavaLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerAddress.class)
public class ServerAddressMixin implements ServerAddressProperties {

    @Shadow
    @Final
    private static ServerAddress INVALID;

    @Unique
    private boolean quic;

    @Inject(method = "parseString(Ljava/lang/String;)Lnet/minecraft/client/multiplayer/resolver/ServerAddress;", at = @At("HEAD"), cancellable = true, remap = false)
    private static void parseString(String input, CallbackInfoReturnable<ServerAddress> callbackInfoReturnable) {
        if (ModConfig.Quic.isDisableQuic() || !JavaLoader.INSTANCE.loaded("kreno_addons_quic")) return;
        var index = input.indexOf("://");

        if (index == -1) {
            return;
        }

        var scheme = input.substring(0, index);
        var newAddress = ServerAddress.parseString(input.substring(index + 3));

        if (newAddress != INVALID) {
            switch (scheme) {
                case "tcp", "minecraft" -> ((ServerAddressProperties) (Object) newAddress).setUseQuic(false);
                case "quic" -> ((ServerAddressProperties) (Object) newAddress).setUseQuic(true);
            }
        }

        callbackInfoReturnable.setReturnValue(newAddress);
    }

    @ModifyVariable(method = "isValidAddress(Ljava/lang/String;)Z", at = @At("HEAD"), argsOnly = true, name = "input")
    private static String modifyIsValidAddress(String input) {
        if (ModConfig.Quic.isDisableQuic() || !JavaLoader.INSTANCE.loaded("kreno_addons_quic")) return input;
        var index = input.indexOf("://");

        if (index == -1) {
            return input;
        } else {
            return input.substring(index + 3);
        }
    }

    @Override
    public boolean getUseQuic() {
        return quic;
    }

    @Override
    public void setUseQuic(boolean quic) {
        this.quic = quic;
    }
}