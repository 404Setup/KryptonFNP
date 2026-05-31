import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.LongAdder;

public class TrafficMonitorBenchmark {

    public static class PacketStat {
        public final LongAdder bytes = new LongAdder();
    }

    public static class PlayerTrafficStat {
        public final String name;
        public final LongAdder totalIn = new LongAdder();
        public final LongAdder totalOut = new LongAdder();

        public PlayerTrafficStat(String name) {
            this.name = name;
        }

        public long getTotal() {
            return totalIn.sum() + totalOut.sum();
        }
    }

    public static final Map<String, PacketStat> inboundStats = new ConcurrentHashMap<>();
    public static final Map<UUID, PlayerTrafficStat> playerStats = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Setup data
        Random r = new Random(42);
        for (int i = 0; i < 1000; i++) {
            PacketStat stat = new PacketStat();
            stat.bytes.add(r.nextInt(10000));
            inboundStats.put("Packet" + i, stat);
        }

        for (int i = 0; i < 1000; i++) {
            PlayerTrafficStat pstat = new PlayerTrafficStat("Player" + i);
            pstat.totalIn.add(r.nextInt(10000));
            pstat.totalOut.add(r.nextInt(10000));
            playerStats.put(UUID.randomUUID(), pstat);
        }

        // Warmup
        for (int i = 0; i < 5000; i++) {
            getTop10InboundStream();
            getTop10InboundPQ();
            getTop10PlayersStream();
            getTop10PlayersPQ();
        }

        // Benchmark Packets
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            getTop10InboundStream();
        }
        System.out.println("Packets Stream: " + (System.nanoTime() - start) / 1_000_000 + "ms");

        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            getTop10InboundPQ();
        }
        System.out.println("Packets PQ: " + (System.nanoTime() - start) / 1_000_000 + "ms");

        // Benchmark Players
        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            getTop10PlayersStream();
        }
        System.out.println("Players Stream: " + (System.nanoTime() - start) / 1_000_000 + "ms");

        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            getTop10PlayersPQ();
        }
        System.out.println("Players PQ: " + (System.nanoTime() - start) / 1_000_000 + "ms");
    }

    public static List<Map.Entry<String, PacketStat>> getTop10InboundStream() {
        return inboundStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().bytes.sum(), a.getValue().bytes.sum()))
                .limit(10).collect(Collectors.toList());
    }

    public static List<Map.Entry<String, PacketStat>> getTop10InboundPQ() {
        PriorityQueue<Map.Entry<String, PacketStat>> pq = new PriorityQueue<>(11,
            Comparator.comparingLong((Map.Entry<String, PacketStat> e) -> e.getValue().bytes.sum()));
        for (Map.Entry<String, PacketStat> entry : inboundStats.entrySet()) {
            pq.offer(entry);
            if (pq.size() > 10) {
                pq.poll();
            }
        }
        List<Map.Entry<String, PacketStat>> result = new ArrayList<>(pq);
        result.sort((a, b) -> Long.compare(b.getValue().bytes.sum(), a.getValue().bytes.sum()));
        return result;
    }

    public static List<PlayerTrafficStat> getTop10PlayersStream() {
        return playerStats.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotal(), a.getTotal()))
                .limit(10).collect(Collectors.toList());
    }

    public static List<PlayerTrafficStat> getTop10PlayersPQ() {
        PriorityQueue<PlayerTrafficStat> pq = new PriorityQueue<>(11,
            Comparator.comparingLong(PlayerTrafficStat::getTotal));
        for (PlayerTrafficStat stat : playerStats.values()) {
            pq.offer(stat);
            if (pq.size() > 10) {
                pq.poll();
            }
        }
        List<PlayerTrafficStat> result = new ArrayList<>(pq);
        result.sort((a, b) -> Long.compare(b.getTotal(), a.getTotal()));
        return result;
    }
}
