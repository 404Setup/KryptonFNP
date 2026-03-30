package one.pkg.kreno.jmh.network;

import org.openjdk.jmh.annotations.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import one.pkg.kreno.shared.network.TrafficMonitor;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class TrafficMonitorBenchmark {

    private UUID playerUuid;
    private String playerName;
    private String packetName;
    private int bytes;

    @Setup(Level.Trial)
    public void setup() {
        playerUuid = UUID.randomUUID();
        playerName = "TestPlayer";
        packetName = "TestPacket";
        bytes = 100;
        TrafficMonitor.reset();
    }

    @Benchmark
    public void testOnOutboundPacket() {
        TrafficMonitor.onOutboundPacket(playerUuid, playerName, packetName, bytes);
    }

    @Benchmark
    public void testOnInboundPacket() {
        TrafficMonitor.onInboundPacket(playerUuid, playerName, packetName, bytes);
    }
}
