package one.pkg.kreno.jmh.network;

import one.pkg.kreno.shared.network.TrafficMonitor;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class TrafficMonitorFormatBytesBenchmark {

    @Benchmark
    public void testFormatBytesOld_1B(Blackhole bh) {
        bh.consume(formatBytesOld(500.0));
    }

    @Benchmark
    public void testFormatBytesOld_1KB(Blackhole bh) {
        bh.consume(formatBytesOld(1500.0));
    }

    @Benchmark
    public void testFormatBytesOld_1MB(Blackhole bh) {
        bh.consume(formatBytesOld(1500000.0));
    }

    @Benchmark
    public void testFormatBytesOpt_1B(Blackhole bh) {
        bh.consume(TrafficMonitor.formatBytes(500.0));
    }

    @Benchmark
    public void testFormatBytesOpt_1KB(Blackhole bh) {
        bh.consume(TrafficMonitor.formatBytes(1500.0));
    }

    @Benchmark
    public void testFormatBytesOpt_1MB(Blackhole bh) {
        bh.consume(TrafficMonitor.formatBytes(1500000.0));
    }

    private String formatBytesOld(double bytes) {
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
