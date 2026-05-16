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
                        TrafficMonitor.formatBytes(TrafficMonitor.totalInUncompressed.sum()),
                        TrafficMonitor.formatBytes(TrafficMonitor.totalOutUncompressed.sum())))
                .append(N);
        if (TrafficMonitor.compressionEnabled) {
            component.append(
                    Component.translatable("kreno.traffic.gui.compressed",
                            TrafficMonitor.formatBytes(TrafficMonitor.totalInCompressed.sum()),
                            TrafficMonitor.formatBytes(TrafficMonitor.totalOutCompressed.sum()))
            ).append(N);
        }

        component.append(
                        Component.translatable("kreno.traffic.gui.rate",
                                TrafficMonitor.formatBytes(TrafficMonitor.inRateBps), TrafficMonitor.inRatePps,
                                TrafficMonitor.formatBytes(TrafficMonitor.outRateBps), TrafficMonitor.outRatePps)
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
                            TrafficMonitor.formatBytes(p.totalOut.sum()),
                            TrafficMonitor.formatBytes(p.totalIn.sum()))
            );
            if (TrafficMonitor.compressionEnabled) {
                component.append(N).append(
                        Component.translatable("kreno.traffic.gui.player_stat_compressed",
                                TrafficMonitor.formatBytes(p.getTotalCompressed()),
                                TrafficMonitor.formatBytes(p.totalOutCompressed.sum()),
                                TrafficMonitor.formatBytes(p.totalInCompressed.sum()))
                );
            }
            if (p.getTotalDropped() > 0) {
                component.append(N).append(
                        Component.translatable("kreno.traffic.gui.player_stat_dropped",
                                TrafficMonitor.formatBytes(p.getTotalDropped()))
                );
                for (Map.Entry<String, TrafficMonitor.PacketStat> entry : p.droppedPackets.entrySet()) {
                    component.append(N).append(
                            Component.translatable("kreno.traffic.gui.player_stat_dropped_detail",
                                    entry.getKey(), TrafficMonitor.formatBytes(entry.getValue().bytes.sum()))
                    );
                }
            }
        }


        component.append(N).append("")
                .append(Component.translatable("kreno.traffic.gui.top_inbound")
                        .withStyle(s -> s.withBold(true).withColor(0xAAAAAA))
                );

        List<Map.Entry<String, TrafficMonitor.PacketStat>> topIn = TrafficMonitor.getTop10Inbound();
        for (int i = 0; i < topIn.size(); i++) {
            Map.Entry<String, TrafficMonitor.PacketStat> entry = topIn.get(i);
            component.append(N).append(Component.translatable("kreno.traffic.gui.packet_stat",
                    (i + 1), entry.getKey(), TrafficMonitor.formatBytes(entry.getValue().bytes.sum())));
        }

        component.append(N).append("")
                .append(Component.translatable("kreno.traffic.gui.top_outbound")
                        .withStyle(s -> s.withBold(true).withColor(0xAAAAAA))
                );

        List<Map.Entry<String, TrafficMonitor.PacketStat>> topOut = TrafficMonitor.getTop10Outbound();
        for (int i = 0; i < topOut.size(); i++) {
            Map.Entry<String, TrafficMonitor.PacketStat> entry = topOut.get(i);
            component.append(N).append(Component.translatable("kreno.traffic.gui.packet_stat",
                    (i + 1), entry.getKey(), TrafficMonitor.formatBytes(entry.getValue().bytes.sum())));
        }

        return new OreUIDialog(Component.translatable("kreno.traffic.gui.title"), lastScreen)
                .cancelText(Component.translatable("kreno.traffic.gui.reset"))
                .onCancel(TrafficMonitor::reset)
                .content(component);
    }
}
