package one.pkg.kreno.shared.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.gui.TrafficMonitorDialog;
import one.pkg.kreno.shared.gui.TrafficMonitorScreen;
import one.pkg.kreno.shared.network.TrafficMonitor;
import one.pkg.libsl.api.loader.JavaLoader;

import java.util.List;

public class KrenoCommand {
    private static SuggestionProvider<CommandSourceStack> suggestPlayers() {
        return (ctx, builder) -> {
            for (Player player : ctx.getSource().getServer().getPlayerList().getPlayers())
                builder.suggest(player.getName().getString());
            return builder.buildFuture();
        };
    }
    public static class Client {
        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
            var cmd = Commands.literal("krenoc")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                    .then(Commands.literal("config")
                            .then(Commands.literal("reload").executes(KrenoCommand::reloadConfig)))
                    .then(Commands.literal("traffic")
                            .then(Commands.literal("all").executes(KrenoCommand::displayAllTraffic))
                            .then(Commands.literal("reset").executes(KrenoCommand::resetTraffic))
                            .then(Commands.literal("gui").executes(_ -> {
                                if (JavaLoader.INSTANCE.isClient()) {
                                    if (ModConfig.GUI.isOreUI())
                                        JavaLoader.INSTANCE.client().setScreen(TrafficMonitorDialog::create);
                                    else
                                        JavaLoader.INSTANCE.client().setScreen(TrafficMonitorScreen::new);
                                    return 1;
                                }
                                return 0;
                            }))
                            .then(Commands.literal("list")
                                    .then(Commands.argument("player", StringArgumentType.string())
                                            .suggests(suggestPlayers())
                                            .executes(KrenoCommand::displayPlayerTraffic)))
                    );

            dispatcher.register(cmd);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var cmd = Commands.literal("kreno")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.literal("config")
                        .then(Commands.literal("reload").executes(KrenoCommand::reloadConfig)))
                .then(Commands.literal("traffic")
                        .then(Commands.literal("all").executes(KrenoCommand::displayAllTraffic))
                        .then(Commands.literal("reset").executes(KrenoCommand::resetTraffic))
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .suggests(suggestPlayers())
                                        .executes(KrenoCommand::displayPlayerTraffic)))
                );

        dispatcher.register(cmd);
    }

    private synchronized static int reloadConfig(CommandContext<CommandSourceStack> context) {
        ModConfig.config.reloadConfigurations(true);
        context.getSource().sendSuccess(() -> Component.literal("Config reloaded successfully").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int resetTraffic(CommandContext<CommandSourceStack> context) {
        TrafficMonitor.reset();
        context.getSource().sendSuccess(() -> Component.translatable("kreno.command.reset.success").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int displayAllTraffic(CommandContext<CommandSourceStack> context) {
        TrafficMonitor.updateRates();
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.translatable("kreno.command.all.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.translatable("kreno.command.all.total",
                TrafficMonitor.formatBytes(TrafficMonitor.totalInUncompressed.sum()),
                TrafficMonitor.formatBytes(TrafficMonitor.totalOutUncompressed.sum())), false);
        source.sendSuccess(() -> Component.translatable("kreno.command.all.rate",
                TrafficMonitor.formatBytes(TrafficMonitor.inRateBps), TrafficMonitor.inRatePps,
                TrafficMonitor.formatBytes(TrafficMonitor.outRateBps), TrafficMonitor.outRatePps), false);

        source.sendSuccess(() -> Component.translatable("kreno.command.all.top_players").withStyle(ChatFormatting.YELLOW), false);
        List<TrafficMonitor.PlayerTrafficStat> topPlayers = TrafficMonitor.getTop10Players();
        for (TrafficMonitor.PlayerTrafficStat p : topPlayers) {
            MutableComponent playerComp = Component.literal(p.name).withStyle(ChatFormatting.AQUA);

            MutableComponent hoverText = Component.translatable("kreno.command.all.hover_header");
            p.outboundPackets.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().bytes.sum(), a.getValue().bytes.sum()))
                    .limit(5)
                    .forEach(e -> hoverText.append(Component.translatable("kreno.command.all.hover_entry", e.getKey(), TrafficMonitor.formatBytes(e.getValue().bytes.sum()))));

            playerComp.withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(hoverText)));

            source.sendSuccess(() -> Component.literal("")
                    .append(playerComp)
                    .append(Component.translatable("kreno.command.all.player_summary",
                            TrafficMonitor.formatBytes(p.getTotal()),
                            TrafficMonitor.formatBytes(p.totalOut.sum()),
                            TrafficMonitor.formatBytes(p.totalIn.sum()))), false);
        }
        return 1;
    }

    private static int displayPlayerTraffic(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "player");
        TrafficMonitor.PlayerTrafficStat stat = TrafficMonitor.playerStats.values().stream()
                .filter(s -> s.name.equalsIgnoreCase(name))
                .findFirst().orElse(null);

        if (stat == null) {
            context.getSource().sendFailure(Component.translatable("kreno.command.player.not_found"));
            return 0;
        }

        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("kreno.command.player.header", stat.name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);

        source.sendSuccess(() -> Component.translatable("kreno.command.player.inbound").withStyle(ChatFormatting.GRAY), false);
        stat.inboundPackets.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().bytes.sum(), a.getValue().bytes.sum()))
                .limit(10)
                .forEach(e -> source.sendSuccess(() -> Component.translatable("kreno.command.player.entry", e.getKey(), TrafficMonitor.formatBytes(e.getValue().bytes.sum()), e.getValue().count.sum()), false));

        source.sendSuccess(() -> Component.translatable("kreno.command.player.outbound").withStyle(ChatFormatting.GRAY), false);
        stat.outboundPackets.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().bytes.sum(), a.getValue().bytes.sum()))
                .limit(10)
                .forEach(e -> source.sendSuccess(() -> Component.translatable("kreno.command.player.entry", e.getKey(), TrafficMonitor.formatBytes(e.getValue().bytes.sum()), e.getValue().count.sum()), false));

        return 1;
    }
}
