package one.pkg.kreno.shared.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import one.pkg.kreno.shared.network.TrafficMonitor;

import java.util.List;
import java.util.Map;

public class TrafficMonitorScreen extends OptionsSubScreen {
    private final Screen lastScreen;

    public TrafficMonitorScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable("kreno.traffic.gui.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void addOptions() {
        if (this.list == null) return;
        TrafficMonitor.updateRates();

        this.list.addHeader(Component.translatable("kreno.traffic.gui.global").withStyle(s -> s.withBold(true).withColor(0xFFFFFF)));
        this.list.addHeader(Component.translatable("kreno.traffic.gui.bytes",
                TrafficMonitor.formatBytes(TrafficMonitor.totalInUncompressed.sum()),
                TrafficMonitor.formatBytes(TrafficMonitor.totalOutUncompressed.sum())));
        
        if (TrafficMonitor.compressionEnabled) {
            this.list.addHeader(Component.translatable("kreno.traffic.gui.compressed",
                    TrafficMonitor.formatBytes(TrafficMonitor.totalInCompressed.sum()),
                    TrafficMonitor.formatBytes(TrafficMonitor.totalOutCompressed.sum())));
        }
        
        this.list.addHeader(Component.translatable("kreno.traffic.gui.rate",
                TrafficMonitor.formatBytes(TrafficMonitor.inRateBps), TrafficMonitor.inRatePps,
                TrafficMonitor.formatBytes(TrafficMonitor.outRateBps), TrafficMonitor.outRatePps));

        this.list.addHeader(Component.literal(""));
        this.list.addHeader(Component.translatable("kreno.traffic.gui.top_players").withStyle(s -> s.withBold(true).withColor(0xAAAAAA)));
        List<TrafficMonitor.PlayerTrafficStat> topPlayers = TrafficMonitor.getTop10Players();
        for (TrafficMonitor.PlayerTrafficStat p : topPlayers) {
            this.list.addHeader(Component.translatable("kreno.traffic.gui.player_stat",
                    p.name, TrafficMonitor.formatBytes(p.getTotal()),
                    TrafficMonitor.formatBytes(p.totalOut.sum()),
                    TrafficMonitor.formatBytes(p.totalIn.sum())));
            if (TrafficMonitor.compressionEnabled) {
                this.list.addHeader(Component.translatable("kreno.traffic.gui.player_stat_compressed",
                        TrafficMonitor.formatBytes(p.getTotalCompressed()),
                        TrafficMonitor.formatBytes(p.totalOutCompressed.sum()),
                        TrafficMonitor.formatBytes(p.totalInCompressed.sum())));
            }
            if (p.getTotalDropped() > 0) {
                this.list.addHeader(Component.translatable("kreno.traffic.gui.player_stat_dropped",
                        TrafficMonitor.formatBytes(p.getTotalDropped())));
                for (Map.Entry<String, TrafficMonitor.PacketStat> entry : p.droppedPackets.entrySet()) {
                    this.list.addHeader(Component.translatable("kreno.traffic.gui.player_stat_dropped_detail",
                            entry.getKey(), TrafficMonitor.formatBytes(entry.getValue().bytes.sum())));
                }
            }
        }
        
        this.list.addHeader(Component.literal(""));
        this.list.addHeader(Component.translatable("kreno.traffic.gui.top_inbound").withStyle(s -> s.withBold(true).withColor(0xAAAAAA)));
        List<Map.Entry<String, TrafficMonitor.PacketStat>> topIn = TrafficMonitor.getTop10Inbound();
        for (int i = 0; i < topIn.size(); i++) {
            Map.Entry<String, TrafficMonitor.PacketStat> entry = topIn.get(i);
            this.list.addHeader(Component.translatable("kreno.traffic.gui.packet_stat",
                    (i + 1), entry.getKey(), TrafficMonitor.formatBytes(entry.getValue().bytes.sum())));
        }

        this.list.addHeader(Component.literal(""));
        this.list.addHeader(Component.translatable("kreno.traffic.gui.top_outbound").withStyle(s -> s.withBold(true).withColor(0xAAAAAA)));
        List<Map.Entry<String, TrafficMonitor.PacketStat>> topOut = TrafficMonitor.getTop10Outbound();
        for (int i = 0; i < topOut.size(); i++) {
            Map.Entry<String, TrafficMonitor.PacketStat> entry = topOut.get(i);
            this.list.addHeader(Component.translatable("kreno.traffic.gui.packet_stat",
                    (i + 1), entry.getKey(), TrafficMonitor.formatBytes(entry.getValue().bytes.sum())));
        }
    }

    @Override
    protected void addFooter() {
        LinearLayout linearLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearLayout.addChild(Button.builder(Component.translatable("kreno.traffic.gui.reset"), (_) -> {
            TrafficMonitor.reset();
            this.repositionElements();
        }).width(100).build());
        linearLayout.addChild(Button.builder(CommonComponents.GUI_DONE, (_) -> this.minecraft.setScreen(this.lastScreen)).width(100).build());
    }
}
