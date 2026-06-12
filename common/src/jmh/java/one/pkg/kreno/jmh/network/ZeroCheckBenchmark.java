package one.pkg.kreno.jmh.network;

import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ZeroCheckBenchmark {

    private byte[] dataZero;
    private byte[] dataRandom;
    private byte[] dataLastByteNonZero;

    private static final byte[] ZERO_ARRAY = new byte[2048];

    @Setup(Level.Trial)
    public void setup() {
        dataZero = new byte[2048];
        dataRandom = new byte[2048];
        new Random(42).nextBytes(dataRandom);
        dataLastByteNonZero = new byte[2048];
        dataLastByteNonZero[2047] = 1;
    }

    @Benchmark
    public boolean baselineZero() {
        return isAllZeroBaseline(dataZero);
    }

    @Benchmark
    public boolean optimizedZero() {
        return isAllZeroOptimized(dataZero);
    }

    @Benchmark
    public boolean baselineRandom() {
        return isAllZeroBaseline(dataRandom);
    }

    @Benchmark
    public boolean optimizedRandom() {
        return isAllZeroOptimized(dataRandom);
    }

    @Benchmark
    public boolean baselineLastByteNonZero() {
        return isAllZeroBaseline(dataLastByteNonZero);
    }

    @Benchmark
    public boolean optimizedLastByteNonZero() {
        return isAllZeroOptimized(dataLastByteNonZero);
    }

    private boolean isAllZeroBaseline(byte[] data) {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllZeroOptimized(byte[] data) {
        if (data.length == 2048) {
            return Arrays.equals(data, ZERO_ARRAY);
        }
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}
