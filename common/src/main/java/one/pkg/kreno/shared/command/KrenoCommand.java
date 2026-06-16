package one.pkg.kreno.shared.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import one.pkg.kreno.shared.ModConfig;

public class KrenoCommand {
    public static class Client {
        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
            var cmd = Commands.literal("krenoc")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                    .then(Commands.literal("config")
                            .then(Commands.literal("reload").executes(KrenoCommand::reloadConfig)));

            dispatcher.register(cmd);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var cmd = Commands.literal("kreno")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.literal("config")
                        .then(Commands.literal("reload").executes(KrenoCommand::reloadConfig)));

        dispatcher.register(cmd);
    }

    private synchronized static int reloadConfig(CommandContext<CommandSourceStack> context) {
        ModConfig.config.reloadConfigurations(true);
        context.getSource().sendSuccess(() -> Component.literal("Config reloaded successfully").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
