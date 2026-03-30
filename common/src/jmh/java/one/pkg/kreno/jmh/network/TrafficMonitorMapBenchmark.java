package one.pkg.kreno.jmh.network;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class TrafficMonitorMapBenchmark {

    private UUID playerUuid;
    private String playerName;
    private String packetName;
    private int bytes;

    public final Map<String, PacketStat> inboundStats = new ConcurrentHashMap<>();
    public final Map<String, PacketStat> outboundStats = new ConcurrentHashMap<>();
    public final Map<UUID, PlayerTrafficStat> playerStats = new ConcurrentHashMap<>();
    public final AtomicLong totalInUncompressed = new AtomicLong();
    public final AtomicLong totalOutUncompressed = new AtomicLong();

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
    }

    @Setup(Level.Trial)
    public void setup() {
        playerUuid = UUID.randomUUID();
        playerName = "TestPlayer";
        packetName = "TestPacket";
        bytes = 100;
    }

    @Benchmark
    public void baseline(Blackhole bh) {
        inboundStats.computeIfAbsent(packetName, k -> new PacketStat()).count.incrementAndGet();
        inboundStats.get(packetName).bytes.addAndGet(bytes);
        totalInUncompressed.addAndGet(bytes);

        if (playerUuid != null) {
            PlayerTrafficStat pStat = playerStats.computeIfAbsent(playerUuid, k -> new PlayerTrafficStat(playerName));
            pStat.inboundPackets.computeIfAbsent(packetName, k -> new PacketStat()).count.incrementAndGet();
            pStat.inboundPackets.get(packetName).bytes.addAndGet(bytes);
            pStat.totalIn.addAndGet(bytes);
        }
    }

    @Benchmark
    public void optimized(Blackhole bh) {
        PacketStat globalStat = inboundStats.computeIfAbsent(packetName, k -> new PacketStat());
        globalStat.count.incrementAndGet();
        globalStat.bytes.addAndGet(bytes);
        totalInUncompressed.addAndGet(bytes);

        if (playerUuid != null) {
            PlayerTrafficStat pStat = playerStats.computeIfAbsent(playerUuid, k -> new PlayerTrafficStat(playerName));
            PacketStat pPacketStat = pStat.inboundPackets.computeIfAbsent(packetName, k -> new PacketStat());
            pPacketStat.count.incrementAndGet();
            pPacketStat.bytes.addAndGet(bytes);
            pStat.totalIn.addAndGet(bytes);
        }
    }
}
