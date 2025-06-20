package me.steinborn.krypton.mixin.shared.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.velocitypowered.natives.util.Natives;
import me.steinborn.krypton.mod.shared.KryptonSharedBootstrap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = DebugScreenOverlay.class, priority = 800)
public class DebugScreenOverlayMixin {
    @Unique
    private final String[] krypton_Multi$text = new String[]{
            ChatFormatting.BLUE + "KryptonFNP Version: " + ChatFormatting.GREEN + KryptonSharedBootstrap.getVersion(),
            ChatFormatting.AQUA + "KryptonFNP Compression: " + ChatFormatting.GREEN + Natives.compress.getLoadedVariant(),
            ChatFormatting.AQUA + "KryptonFNP Encrypt: " + ChatFormatting.GREEN + Natives.cipher.getLoadedVariant()
    };

    @ModifyReturnValue(method = "getGameInformation", at = @At("RETURN"))
    private List<String> krypton_fnp$add(List<String> list) {
        if (!list.getLast().isEmpty()) list.add("");
        list.add(krypton_Multi$text[0]);
        list.add(krypton_Multi$text[1]);
        list.add(krypton_Multi$text[2]);
        list.add("");
        return list;
    }
}
