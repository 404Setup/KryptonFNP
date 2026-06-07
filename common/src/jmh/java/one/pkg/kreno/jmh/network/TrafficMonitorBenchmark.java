package one.pkg.kreno.jmh.network;

import one.pkg.kreno.shared.network.TrafficMonitor;
import org.openjdk.jmh.annotations.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1)
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
    public void baseline() {
        TrafficMonitor.onInboundPacket(playerUuid, playerName, packetName, bytes);
        TrafficMonitor.onOutboundPacket(playerUuid, playerName, packetName, bytes);
    }
}
