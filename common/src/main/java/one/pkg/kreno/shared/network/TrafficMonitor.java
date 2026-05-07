package one.pkg.kreno.shared.network;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class TrafficMonitor {
    public static final Map<String, PacketStat> inboundStats = new ConcurrentHashMap<>();
    public static final Map<String, PacketStat> outboundStats = new ConcurrentHashMap<>();
    public static final Map<UUID, PlayerTrafficStat> playerStats = new ConcurrentHashMap<>();
    public static final LongAdder totalInUncompressed = new LongAdder();
    public static final LongAdder totalOutUncompressed = new LongAdder();
    public static final LongAdder totalInCompressed = new LongAdder();
    public static final LongAdder totalOutCompressed = new LongAdder();
    public static final LongAdder totalInPackets = new LongAdder();
    public static final LongAdder totalOutPackets = new LongAdder();
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
        totalInUncompressed.reset();
        totalOutUncompressed.reset();
        totalInCompressed.reset();
        totalOutCompressed.reset();
        totalInPackets.reset();
        totalOutPackets.reset();
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
        PacketStat globalStat = inboundStats.get(packetName);
        if (globalStat == null) {
            globalStat = inboundStats.computeIfAbsent(packetName, k -> new PacketStat());
        }
        globalStat.count.increment();
        globalStat.bytes.add(bytes);
        totalInUncompressed.add(bytes);
        totalInPackets.increment();

        if (playerUuid != null) {
            PlayerTrafficStat pStat = playerStats.get(playerUuid);
            if (pStat == null) {
                pStat = playerStats.computeIfAbsent(playerUuid, k -> new PlayerTrafficStat(playerName));
            }
            PacketStat pPacketStat = pStat.inboundPackets.get(packetName);
            if (pPacketStat == null) {
                pPacketStat = pStat.inboundPackets.computeIfAbsent(packetName, k -> new PacketStat());
            }
            pPacketStat.count.increment();
            pPacketStat.bytes.add(bytes);
            pStat.totalIn.add(bytes);
        }
        updateRates();
    }

    public static void onOutboundPacket(UUID playerUuid, String playerName, String packetName, int bytes) {
        PacketStat globalStat = outboundStats.get(packetName);
        if (globalStat == null) {
            globalStat = outboundStats.computeIfAbsent(packetName, k -> new PacketStat());
        }
        globalStat.count.increment();
        globalStat.bytes.add(bytes);
        totalOutUncompressed.add(bytes);
        totalOutPackets.increment();

        if (playerUuid != null) {
            PlayerTrafficStat pStat = playerStats.get(playerUuid);
            if (pStat == null) {
                pStat = playerStats.computeIfAbsent(playerUuid, k -> new PlayerTrafficStat(playerName));
            }
            PacketStat pPacketStat = pStat.outboundPackets.get(packetName);
            if (pPacketStat == null) {
                pPacketStat = pStat.outboundPackets.computeIfAbsent(packetName, k -> new PacketStat());
            }
            pPacketStat.count.increment();
            pPacketStat.bytes.add(bytes);
            pStat.totalOut.add(bytes);
        }
        updateRates();
    }

    public static void onInboundCompressed(int bytes) {
        totalInCompressed.add(bytes);
    }

    public static void onOutboundCompressed(int bytes) {
        totalOutCompressed.add(bytes);
    }

    public static void updateRates() {
        long now = System.currentTimeMillis();
        long diff = now - lastTime;
        if (diff >= 1000) {
            long totalIn = compressionEnabled ? totalInCompressed.sum() : totalInUncompressed.sum();
            long totalOut = compressionEnabled ? totalOutCompressed.sum() : totalOutUncompressed.sum();
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
        return totalInPackets.sum();
    }

    public static long getTotalOutPackets() {
        return totalOutPackets.sum();
    }

    public static List<Map.Entry<String, PacketStat>> getTop10Inbound() {
        return inboundStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().bytes.sum(), a.getValue().bytes.sum()))
                .limit(10).collect(Collectors.toList());
    }

    public static List<Map.Entry<String, PacketStat>> getTop10Outbound() {
        return outboundStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().bytes.sum(), a.getValue().bytes.sum()))
                .limit(10).collect(Collectors.toList());
    }

    public static List<PlayerTrafficStat> getTop10Players() {
        return playerStats.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotal(), a.getTotal()))
                .limit(10).collect(Collectors.toList());
    }

    public static class PacketStat {
        public final LongAdder count = new LongAdder();
        public final LongAdder bytes = new LongAdder();
    }

    public static class PlayerTrafficStat {
        public final String name;
        public final Map<String, PacketStat> inboundPackets = new ConcurrentHashMap<>();
        public final Map<String, PacketStat> outboundPackets = new ConcurrentHashMap<>();
        public final LongAdder totalIn = new LongAdder();
        public final LongAdder totalOut = new LongAdder();

        public PlayerTrafficStat(String name) {
            this.name = name;
        }

        public long getTotal() {
            return totalIn.sum() + totalOut.sum();
        }
    }

    public static String formatBytes(double bytes) {
        if (bytes < 1024) return String.format("%.2f B", bytes);
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
