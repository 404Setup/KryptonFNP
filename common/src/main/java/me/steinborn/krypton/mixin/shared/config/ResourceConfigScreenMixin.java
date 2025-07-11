package me.steinborn.krypton.mixin.shared.config;

import com.llamalad7.mixinextras.sugar.Local;
import net.xstopho.resourceconfigapi.client.gui.screen.ResourceConfigScreen;
import net.xstopho.resourceconfigapi.client.util.ClientUtils;
import net.xstopho.resourceconfigapi.config.ConfigHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ResourceConfigScreen.class, remap = false)
public class ResourceConfigScreenMixin {
    @Shadow
    private void saveConfig(ConfigHolder holder) {
    }

    @Redirect(method = "saveConfigChanges", at = @At(value = "INVOKE", target = "Lnet/xstopho/resourceconfigapi/client/util/ClientUtils;sendConfigUpdateToServer(Ljava/lang/String;Ljava/lang/String;)V"))
    public void no_krypton_fnp(String file, String json, @Local(argsOnly = true) ConfigHolder holder) {
        if (file.equals("krypton:common/krypton_fnp")) {
            saveConfig(holder);
        } else {
            ClientUtils.sendConfigUpdateToServer(file, json);
        }
    }
}
