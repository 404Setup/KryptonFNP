package one.pkg.kreno.mixin.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import one.pkg.kreno.shared.command.KrenoCommand;
import one.pkg.kreno.shared.network.TrafficMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class KrenoClientCommandsMixin {
    @Shadow
    private CommandDispatcher<CommandSourceStack> commands;

    @Inject(method = "handleCommands", at = @At("TAIL"))
    private void kreno$onHandleCommands(CallbackInfo ci) {
        KrenoCommand.register(this.commands);
    }
}
