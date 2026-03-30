package one.pkg.kreno.shared.network;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TrafficMonitor {
    public static final Map<String, PacketStat> inboundStats = new ConcurrentHashMap<>();
    public static final Map<String, PacketStat> outboundStats = new ConcurrentHashMap<>();
    public static final Map<UUID, PlayerTrafficStat> playerStats = new ConcurrentHashMap<>();
    public static final AtomicLong totalInUncompressed = new AtomicLong();
    public static final AtomicLong totalOutUncompressed = new AtomicLong();
    public static final AtomicLong totalInCompressed = new AtomicLong();
    public static final AtomicLong totalOutCompressed = new AtomicLong();
    public static final AtomicLong totalInPackets = new AtomicLong();
    public static final AtomicLong totalOutPackets = new AtomicLong();
    public static double inRateBps = 0;
    public static double outRateBps = 0;
    public static double inRatePps = 0;
    public static double outRatePps = 0;
    public static boolean compressionEnabled = false;
    private static long lastTime = System.currentTimeMillis();
    private static long lastTotalIn;
    private static long lastTotalOut;
    private static long lastPacketsIn;
    private static long lastPacketsOut;

    public static void reset() {
        inboundStats.clear();
        outboundStats.clear();
        playerStats.clear();
        totalInUncompressed.set(0);
        totalOutUncompressed.set(0);
        totalInCompressed.set(0);
        totalOutCompressed.set(0);
        totalInPackets.set(0);
        totalOutPackets.set(0);
        inRateBps = 0;
        outRateBps = 0;
        inRatePps = 0;
        outRatePps = 0;
        lastTotalIn = 0;
        lastTotalOut = 0;
        lastPacketsIn = 0;
        lastPacketsOut = 0;
        lastTime = System.currentTimeMillis();
    }

    public static void onInboundPacket(UUID playerUuid, String playerName, String packetName, int bytes) {
        PacketStat globalStat = inboundStats.computeIfAbsent(packetName, k -> new PacketStat());
        globalStat.count.incrementAndGet();
        globalStat.bytes.addAndGet(bytes);
        totalInUncompressed.addAndGet(bytes);
        totalInPackets.incrementAndGet();

        if (playerUuid != null) {
            PlayerTrafficStat pStat = playerStats.computeIfAbsent(playerUuid, k -> new PlayerTrafficStat(playerName));
            PacketStat pPacketStat = pStat.inboundPackets.computeIfAbsent(packetName, k -> new PacketStat());
            pPacketStat.count.incrementAndGet();
            pPacketStat.bytes.addAndGet(bytes);
            pStat.totalIn.addAndGet(bytes);
        }
        updateRates();
    }

    public static void onOutboundPacket(UUID playerUuid, String playerName, String packetName, int bytes) {
        PacketStat globalStat = outboundStats.computeIfAbsent(packetName, _ -> new PacketStat());
        globalStat.count.incrementAndGet();
        globalStat.bytes.addAndGet(bytes);
        totalOutUncompressed.addAndGet(bytes);
        totalOutPackets.incrementAndGet();

        if (playerUuid != null) {
            PlayerTrafficStat pStat = playerStats.computeIfAbsent(playerUuid, k -> new PlayerTrafficStat(playerName));
            PacketStat pPacketStat = pStat.outboundPackets.computeIfAbsent(packetName, k -> new PacketStat());
            pPacketStat.count.incrementAndGet();
            pPacketStat.bytes.addAndGet(bytes);
            pStat.totalOut.addAndGet(bytes);
        }
        updateRates();
    }

    public static void onInboundCompressed(int bytes) {
        totalInCompressed.addAndGet(bytes);
    }

    public static void onOutboundCompressed(int bytes) {
        totalOutCompressed.addAndGet(bytes);
    }

    public static void updateRates() {
        long now = System.currentTimeMillis();
        long diff = now - lastTime;
        if (diff >= 1000) {
            long totalIn = compressionEnabled ? totalInCompressed.get() : totalInUncompressed.get();
            long totalOut = compressionEnabled ? totalOutCompressed.get() : totalOutUncompressed.get();
            long packetsIn = getTotalInPackets();
            long packetsOut = getTotalOutPackets();

            inRateBps = (totalIn - lastTotalIn) * 1000.0 / diff;
            outRateBps = (totalOut - lastTotalOut) * 1000.0 / diff;
            inRatePps = (packetsIn - lastPacketsIn) * 1000.0 / diff;
            outRatePps = (packetsOut - lastPacketsOut) * 1000.0 / diff;

            lastTotalIn = totalIn;
            lastTotalOut = totalOut;
            lastPacketsIn = packetsIn;
            lastPacketsOut = packetsOut;
            lastTime = now;
        }
    }

    public static long getTotalInPackets() {
        return totalInPackets.get();
    }

    public static long getTotalOutPackets() {
        return totalOutPackets.get();
    }

    public static List<Map.Entry<String, PacketStat>> getTop10Inbound() {
        return inboundStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().bytes.get(), a.getValue().bytes.get()))
                .limit(10).collect(Collectors.toList());
    }

    public static List<Map.Entry<String, PacketStat>> getTop10Outbound() {
        return outboundStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().bytes.get(), a.getValue().bytes.get()))
                .limit(10).collect(Collectors.toList());
    }

    public static List<PlayerTrafficStat> getTop10Players() {
        return playerStats.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotal(), a.getTotal()))
                .limit(10).collect(Collectors.toList());
    }

    public static class PacketStat {
        public final AtomicLong count = new AtomicLong();
        public final AtomicLong bytes = new AtomicLong();
    }

    public static class PlayerTrafficStat {
        public final String name;
        public final Map<String, PacketStat> inboundPackets = new ConcurrentHashMap<>();
        public final Map<String, PacketStat> outboundPackets = new ConcurrentHashMap<>();
        public final AtomicLong totalIn = new AtomicLong();
        public final AtomicLong totalOut = new AtomicLong();

        public PlayerTrafficStat(String name) {
            this.name = name;
        }

        public long getTotal() {
            return totalIn.get() + totalOut.get();
        }
    }

    public static String formatBytes(double bytes) {
        if (bytes < 1024) return String.format("%.0f B", bytes);
        int exp = 0;
        double b = bytes;
        while (b >= 1024 && exp < 6) {
            b /= 1024;
            exp++;
        }
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", b, pre);
    }
}
