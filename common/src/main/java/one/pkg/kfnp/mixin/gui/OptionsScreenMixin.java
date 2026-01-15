package one.pkg.kfnp.mixin.gui;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import one.pkg.loader.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", shift = At.Shift.BEFORE, ordinal = 11))
    private void addConfigOption(CallbackInfo ci, @Local GridLayout.RowHelper gridlayout$rowhelper) {
        gridlayout$rowhelper.addChild(Button.builder(Component.literal("Config KryptonFNP"), (button) -> {
            Util.getPlatform().openUri(Loader.INSTANCE.getConfigPath().resolve("krypton_fnp.yaml").toUri());
        }).build());
    }
}
