package one.pkg.kreno.shared.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import one.pkg.kreno.shared.network.TrafficMonitor;
import one.pkg.libsl.api.ui.oreui.OreUIDialog;

import java.util.List;
import java.util.Map;

public class TrafficMonitorDialog {
    private static final Component N = Component.literal("\n");

    public static Screen create(Screen lastScreen) {
        TrafficMonitor.updateRates();
        MutableComponent component = Component.empty()
                .append(
                        Component.translatable("kreno.traffic.gui.global")
                                .withStyle(s -> s.withBold(true).withColor(0xFFFFFF))
                )
                .append(N)
                .append(Component.translatable("kreno.traffic.gui.bytes",
                        TrafficMonitor.formatBytes(TrafficMonitor.totalInUncompressed.get()),
                        TrafficMonitor.formatBytes(TrafficMonitor.totalOutUncompressed.get())))
                .append(N);
        if (TrafficMonitor.compressionEnabled) {
            component.append(
                    Component.translatable("kreno.traffic.gui.compressed",
                            TrafficMonitor.formatBytes(TrafficMonitor.totalInCompressed.get()),
                            TrafficMonitor.formatBytes(TrafficMonitor.totalOutCompressed.get()))
            ).append(N);
        }

        component.append(
                        Component.translatable("kreno.traffic.gui.rate",
                                TrafficMonitor.formatBytes(TrafficMonitor.inRateBps),
                                TrafficMonitor.formatBytes(TrafficMonitor.outRateBps))
                )
                .append(N)
                .append("")
                .append(Component.translatable("kreno.traffic.gui.top_players")
                        .withStyle(s -> s.withBold(true).withColor(0xAAAAAA))
                );

        List<TrafficMonitor.PlayerTrafficStat> topPlayers = TrafficMonitor.getTop10Players();
        for (TrafficMonitor.PlayerTrafficStat p : topPlayers) {
            component.append(N).append(
                    Component.translatable("kreno.traffic.gui.player_stat",
                            p.name, TrafficMonitor.formatBytes(p.getTotal()),
                            TrafficMonitor.formatBytes(p.totalOut.get()),
                            TrafficMonitor.formatBytes(p.totalIn.get()))
            );
        }


        component.append(N).append("")
                .append(Component.translatable("kreno.traffic.gui.top_inbound")
                        .withStyle(s -> s.withBold(true).withColor(0xAAAAAA))
                );

        List<Map.Entry<String, TrafficMonitor.PacketStat>> topIn = TrafficMonitor.getTop10Inbound();
        for (int i = 0; i < topIn.size(); i++) {
            Map.Entry<String, TrafficMonitor.PacketStat> entry = topIn.get(i);
            component.append(N).append(Component.translatable("kreno.traffic.gui.packet_stat",
                    (i + 1), entry.getKey(), TrafficMonitor.formatBytes(entry.getValue().bytes.get())));
        }

        return new OreUIDialog(Component.translatable("kreno.traffic.gui.title"), lastScreen)
                .cancelText(Component.translatable("kreno.traffic.gui.reset"))
                .onCancel(TrafficMonitor::reset)
                .content(component);
    }
}
