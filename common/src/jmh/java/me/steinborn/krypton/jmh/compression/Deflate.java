package me.steinborn.krypton.jmh.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Deflate extends DataBase {

    @Param({"SMALL", "MEDIUM", "LARGE"})
    private DataSize dataSize;

    @Param({"REPETITIVE", "MINECRAFT_LIKE"})
    private DataType dataType;

    @Param({"15", "75"})
    private int batchSize;

    @Param({"128", "512"})
    private int compressionThreshold;

    private Map<String, TestDataBatch> testDataBatches;
    private VelocityCompressor velocityCompressor;
    private Deflater vanillaDeflater;

    @Setup(Level.Trial)
    public void setup() {
        testDataBatches = new HashMap<>();
        prepareTestDataBatches();
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        if (velocityCompressor != null) {
            velocityCompressor.close();
        }
        velocityCompressor = Natives.compress.get().create(4);

        if (vanillaDeflater != null) {
            vanillaDeflater.end();
        }
        vanillaDeflater = new Deflater();
    }

    private void prepareTestDataBatches() {
        for (DataSize size : DataSize.values()) {
            for (DataType type : DataType.values()) {
                if (type == DataType.RANDOM || type == DataType.MIXED) {
                    continue;
                }

                for (int threshold : new int[]{128, 256, 512}) {
                    String key = size.name() + "_" + type.name() + "_" + batchSize + "_" + threshold;
                    TestDataBatch batch = new TestDataBatch(batchSize);

                    for (int i = 0; i < batchSize; i++) {
                        byte[] testData = generateTestData(size, type, i);
                        batch.testData[i] = testData;
                        batch.shouldCompress[i] = testData.length >= threshold;
                    }

                    testDataBatches.put(key, batch);
                }
            }
        }
    }

    private byte[] compressWithVelocity(byte[] data) {
        ByteBuf inputBuf = MoreByteBufUtils.ensureCompatible(ByteBufAllocator.DEFAULT, velocityCompressor,
                Unpooled.wrappedBuffer(data));
        ByteBuf outputBuf = MoreByteBufUtils.preferredBuffer(ByteBufAllocator.DEFAULT, velocityCompressor,
                data.length + 64);

        try {
            velocityCompressor.deflate(inputBuf, outputBuf);
            byte[] result = new byte[outputBuf.readableBytes()];
            outputBuf.readBytes(result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("VelocityCompressor compression failed", e);
        } finally {
            inputBuf.release();
            outputBuf.release();
        }
    }

    private byte[] createMinecraftPacketVelocity(byte[] data, boolean compress) {
        ByteBuf output = Unpooled.buffer();

        try {
            if (!compress) {
                writeVarInt(output, 0);
                output.writeBytes(data);
            } else {
                writeVarInt(output, data.length);

                byte[] compressed = compressWithVelocity(data);
                output.writeBytes(compressed);
            }

            byte[] result = new byte[output.readableBytes()];
            output.readBytes(result);
            return result;
        } finally {
            output.release();
        }
    }

    private byte[] createMinecraftPacketJava(byte[] data, boolean compress) {
        ByteBuf output = Unpooled.buffer();

        try {
            if (!compress) {
                writeVarInt(output, 0);
                output.writeBytes(data);
            } else {
                writeVarInt(output, data.length);

                vanillaDeflater.reset();
                vanillaDeflater.setInput(data);
                vanillaDeflater.finish();

                byte[] compressedBytes = new byte[data.length + 64];
                int compressedLength = vanillaDeflater.deflate(compressedBytes);
                output.writeBytes(compressedBytes, 0, compressedLength);
            }

            byte[] result = new byte[output.readableBytes()];
            output.readBytes(result);
            return result;
        } finally {
            output.release();
        }
    }

    @Benchmark
    public byte[][] velocityDeflateBatch() {
        String key = dataSize.name() + "_" + dataType.name() + "_" + batchSize + "_" + compressionThreshold;
        TestDataBatch batch = testDataBatches.get(key);
        byte[][] results = new byte[batchSize][];

        for (int i = 0; i < batchSize; i++) {
            byte[] testData = batch.testData[i];
            boolean shouldCompress = batch.shouldCompress[i];
            results[i] = createMinecraftPacketVelocity(testData, shouldCompress);
        }

        return results;
    }

    @Benchmark
    public byte[][] javaDeflateBatch() {
        String key = dataSize.name() + "_" + dataType.name() + "_" + batchSize + "_" + compressionThreshold;
        TestDataBatch batch = testDataBatches.get(key);
        byte[][] results = new byte[batchSize][];

        for (int i = 0; i < batchSize; i++) {
            byte[] testData = batch.testData[i];
            boolean shouldCompress = batch.shouldCompress[i];
            results[i] = createMinecraftPacketJava(testData, shouldCompress);
        }

        return results;
    }

    @Benchmark
    public byte[][] velocityCompressorOnly() {
        String key = dataSize.name() + "_" + dataType.name() + "_" + batchSize + "_" + compressionThreshold;
        TestDataBatch batch = testDataBatches.get(key);
        byte[][] results = new byte[batchSize][];

        for (int i = 0; i < batchSize; i++) {
            byte[] testData = batch.testData[i];
            boolean shouldCompress = batch.shouldCompress[i];

            if (!shouldCompress) {
                results[i] = testData.clone();
            } else {
                results[i] = compressWithVelocity(testData);
            }
        }

        return results;
    }

    @Benchmark
    public byte[][] javaCompressorOnly() {
        String key = dataSize.name() + "_" + dataType.name() + "_" + batchSize + "_" + compressionThreshold;
        TestDataBatch batch = testDataBatches.get(key);
        byte[][] results = new byte[batchSize][];

        for (int i = 0; i < batchSize; i++) {
            byte[] testData = batch.testData[i];
            boolean shouldCompress = batch.shouldCompress[i];

            if (!shouldCompress) {
                results[i] = testData.clone();
            } else {
                vanillaDeflater.reset();
                vanillaDeflater.setInput(testData);
                vanillaDeflater.finish();

                byte[] compressedBytes = new byte[testData.length + 64];
                int compressedLength = vanillaDeflater.deflate(compressedBytes);

                byte[] result = new byte[compressedLength];
                System.arraycopy(compressedBytes, 0, result, 0, compressedLength);
                results[i] = result;
            }
        }

        return results;
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        if (velocityCompressor != null) {
            velocityCompressor.close();
            velocityCompressor = null;
        }
        if (vanillaDeflater != null) {
            vanillaDeflater.end();
            vanillaDeflater = null;
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (testDataBatches != null) {
            testDataBatches.clear();
        }
    }

    private static class TestDataBatch {
        final byte[][] testData;
        final boolean[] shouldCompress;

        TestDataBatch(int batchSize) {
            this.testData = new byte[batchSize][];
            this.shouldCompress = new boolean[batchSize];
        }
    }
}