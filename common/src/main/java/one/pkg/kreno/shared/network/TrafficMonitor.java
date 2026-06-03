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
    public static final Map<String, UUID> playerNameCache = new ConcurrentHashMap<>();
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
        playerNameCache.clear();
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
                pStat = playerStats.computeIfAbsent(playerUuid, k -> {
                    if (playerName != null) {
                        playerNameCache.putIfAbsent(playerName.toLowerCase(java.util.Locale.ROOT), playerUuid);
                    }
                    return new PlayerTrafficStat(playerName);
                });
            }
            PacketStat pPacketStat = pStat.inboundPackets.get(packetName);
            if (pPacketStat == null) {
                pPacketStat = pStat.inboundPackets.computeIfAbsent(packetName, k -> new PacketStat());
            }
            pPacketStat.count.increment();
            pPacketStat.bytes.add(bytes);
            pStat.totalIn.add(bytes);
        }
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
                pStat = playerStats.computeIfAbsent(playerUuid, k -> {
                    if (playerName != null) {
                        playerNameCache.putIfAbsent(playerName.toLowerCase(java.util.Locale.ROOT), playerUuid);
                    }
                    return new PlayerTrafficStat(playerName);
                });
            }
            PacketStat pPacketStat = pStat.outboundPackets.get(packetName);
            if (pPacketStat == null) {
                pPacketStat = pStat.outboundPackets.computeIfAbsent(packetName, k -> new PacketStat());
            }
            pPacketStat.count.increment();
            pPacketStat.bytes.add(bytes);
            pStat.totalOut.add(bytes);
        }
    }

    public static void onInboundCompressed(UUID playerUuid, String playerName, int bytes) {
        totalInCompressed.add(bytes);
        if (playerUuid != null) {
            PlayerTrafficStat pStat = playerStats.get(playerUuid);
            if (pStat == null) {
                pStat = playerStats.computeIfAbsent(playerUuid, k -> {
                    if (playerName != null) {
                        playerNameCache.putIfAbsent(playerName.toLowerCase(java.util.Locale.ROOT), playerUuid);
                    }
                    return new PlayerTrafficStat(playerName);
                });
            }
            pStat.totalInCompressed.add(bytes);
        }
    }

    public static void onOutboundCompressed(UUID playerUuid, String playerName, int bytes) {
        totalOutCompressed.add(bytes);
        if (playerUuid != null) {
            PlayerTrafficStat pStat = playerStats.get(playerUuid);
            if (pStat == null) {
                pStat = playerStats.computeIfAbsent(playerUuid, k -> {
                    if (playerName != null) {
                        playerNameCache.putIfAbsent(playerName.toLowerCase(java.util.Locale.ROOT), playerUuid);
                    }
                    return new PlayerTrafficStat(playerName);
                });
            }
            pStat.totalOutCompressed.add(bytes);
        }
    }

    public static void onDroppedPacket(UUID playerUuid, String reason, int estimatedBytes) {
        if (playerUuid != null) {
            PlayerTrafficStat pStat = playerStats.get(playerUuid);
            if (pStat != null) {
                PacketStat pPacketStat = pStat.droppedPackets.computeIfAbsent(reason, k -> new PacketStat());
                pPacketStat.count.increment();
                pPacketStat.bytes.add(estimatedBytes);
                pStat.totalDropped.add(estimatedBytes);
            }
        }
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

    private static class CachedPacketStatEntry implements Comparable<CachedPacketStatEntry> {
        final Map.Entry<String, PacketStat> entry;
        final long sum;

        CachedPacketStatEntry(Map.Entry<String, PacketStat> entry) {
            this.entry = entry;
            this.sum = entry.getValue().bytes.sum();
        }

        @Override
        public int compareTo(CachedPacketStatEntry o) {
            return Long.compare(this.sum, o.sum);
        }
    }

    private static class CachedPlayerStat implements Comparable<CachedPlayerStat> {
        final PlayerTrafficStat stat;
        final long total;

        CachedPlayerStat(PlayerTrafficStat stat) {
            this.stat = stat;
            this.total = stat.getTotal();
        }

        @Override
        public int compareTo(CachedPlayerStat o) {
            return Long.compare(this.total, o.total);
        }
    }

    private static <T, C extends Comparable<C>> List<T> getTopN(Iterable<T> source, int n, java.util.function.Function<T, C> wrapper, java.util.function.Function<C, T> unwrapper) {
        java.util.PriorityQueue<C> pq = new java.util.PriorityQueue<>(n + 1);
        for (T item : source) {
            pq.offer(wrapper.apply(item));
            if (pq.size() > n) {
                pq.poll();
            }
        }
        List<C> cachedResult = new java.util.ArrayList<>(pq);
        cachedResult.sort(java.util.Collections.reverseOrder());
        List<T> result = new java.util.ArrayList<>(cachedResult.size());
        for (C ce : cachedResult) {
            result.add(unwrapper.apply(ce));
        }
        return result;
    }

    public static List<Map.Entry<String, PacketStat>> getTop10Inbound() {
        return getTopN(inboundStats.entrySet(), 10, CachedPacketStatEntry::new, c -> c.entry);
    }

    public static List<Map.Entry<String, PacketStat>> getTop10Outbound() {
        return getTopN(outboundStats.entrySet(), 10, CachedPacketStatEntry::new, c -> c.entry);
    }

    public static List<PlayerTrafficStat> getTop10Players() {
        return getTopN(playerStats.values(), 10, CachedPlayerStat::new, c -> c.stat);
    }

    public static class PacketStat {
        public final LongAdder count = new LongAdder();
        public final LongAdder bytes = new LongAdder();
    }

    public static class PlayerTrafficStat {
        public final String name;
        public final Map<String, PacketStat> inboundPackets = new ConcurrentHashMap<>();
        public final Map<String, PacketStat> outboundPackets = new ConcurrentHashMap<>();
        public final Map<String, PacketStat> droppedPackets = new ConcurrentHashMap<>();
        public final LongAdder totalIn = new LongAdder();
        public final LongAdder totalOut = new LongAdder();
        public final LongAdder totalInCompressed = new LongAdder();
        public final LongAdder totalOutCompressed = new LongAdder();
        public final LongAdder totalDropped = new LongAdder();

        public PlayerTrafficStat(String name) {
            this.name = name;
        }

        public long getTotal() {
            return totalIn.sum() + totalOut.sum();
        }
        
        public long getTotalCompressed() {
            return totalInCompressed.sum() + totalOutCompressed.sum();
        }
        
        public long getTotalDropped() {
            return totalDropped.sum();
        }
    }

    private static final String[] SUFFIXES = {" B", " KB", " MB", " GB", " TB", " PB", " EB"};

    public static String formatBytes(double bytes) {
        if (Double.isNaN(bytes)) return "NaN B";
        if (Double.isInfinite(bytes)) return (bytes > 0 ? "Infinity" : "-Infinity") + " B";

        int exp = 0;
        double b = Math.abs(bytes);
        while (b >= 1024 && exp < 6) {
            b /= 1024;
            exp++;
        }

        double d = bytes < 0 ? -b : b;
        long whole = (long) d;
        long frac = Math.round((Math.abs(d) - Math.abs(whole)) * 100);
        if (frac == 100) {
            whole += (d < 0 ? -1 : 1);
            frac = 0;
        }

        String prefix = (d < 0 && whole == 0) ? "-0" : Long.toString(whole);

        if (frac < 10) {
            return prefix + ".0" + frac + SUFFIXES[exp];
        } else {
            return prefix + "." + frac + SUFFIXES[exp];
        }
    }
}
