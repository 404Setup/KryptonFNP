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
public class TrafficMonitorTop10Benchmark {

    @Setup(Level.Trial)
    public void setup() {
        TrafficMonitor.reset();
        for (int i = 0; i < 1000; i++) {
            UUID playerUuid = UUID.randomUUID();
            String playerName = "TestPlayer" + i;
            String packetName = "TestPacket" + (i % 50);
            TrafficMonitor.onInboundPacket(playerUuid, playerName, packetName, i * 10);
            TrafficMonitor.onOutboundPacket(playerUuid, playerName, packetName, i * 5);
        }
    }

    @Benchmark
    public Object testTop10Inbound() {
        return TrafficMonitor.getTop10Inbound();
    }

    @Benchmark
    public Object testTop10Outbound() {
        return TrafficMonitor.getTop10Outbound();
    }

    @Benchmark
    public Object testTop10Players() {
        return TrafficMonitor.getTop10Players();
    }
}
