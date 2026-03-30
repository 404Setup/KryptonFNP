package one.pkg.kreno.jmh.network;

import one.pkg.kreno.shared.network.TrafficMonitor;
import org.openjdk.jmh.annotations.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class TrafficMonitorBenchmark {

    private UUID playerUuid;
    private String playerName;
    private String packetName;

    @Setup(Level.Trial)
    public void setup() {
        playerUuid = UUID.randomUUID();
        playerName = "TestPlayer";
        packetName = "ClientboundKeepAlivePacket";
        TrafficMonitor.reset();
    }

    @Benchmark
    public void testOnInboundPacket() {
        TrafficMonitor.onInboundPacket(playerUuid, playerName, packetName, 128);
    }

    @Benchmark
    public void testOnOutboundPacket() {
        TrafficMonitor.onOutboundPacket(playerUuid, playerName, packetName, 128);
    }
}
