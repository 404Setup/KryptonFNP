package one.pkg.mod.krypton_fnp.jmh.compression;

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
import java.util.zip.Inflater;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Inflate extends DataBase {

    @Param({"SMALL", "MEDIUM", "LARGE"})
    private DataSize dataSize;

    @Param({"REPETITIVE", "MINECRAFT_LIKE"})
    private DataType dataType;

    @Param({"15", "75"})
    private int batchSize;

    private Map<String, InflateDataBatch> compressedDataBatches;
    private VelocityCompressor velocityCompressor;
    private Inflater vanillaInflater;

    @Setup(Level.Trial)
    public void setup() {

        compressedDataBatches = new HashMap<>();
        prepareCompressedDataBatches();
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        if (velocityCompressor != null) {
            velocityCompressor.close();
        }
        velocityCompressor = Natives.compress.get().create(4);

        if (vanillaInflater != null) {
            vanillaInflater.end();
        }
        vanillaInflater = new Inflater();
    }

    private void prepareCompressedDataBatches() {
        try (VelocityCompressor tempCompressor = Natives.compress.get().create(4)) {
            for (DataSize size : DataSize.values()) {
                for (DataType type : DataType.values()) {
                    if (type == DataType.RANDOM || type == DataType.MIXED) {
                        continue;
                    }

                    String key = size.name() + "_" + type.name() + "_" + batchSize;
                    InflateDataBatch batch = new InflateDataBatch(batchSize);

                    for (int i = 0; i < batchSize; i++) {
                        byte[] testData = generateTestData(size, type, i);
                        batch.uncompressedSizes[i] = testData.length;
                        batch.velocityCompressed[i] = compressWithVelocity(tempCompressor, testData);
                        batch.vanillaCompressed[i] = compressWithJava(testData);
                    }

                    compressedDataBatches.put(key, batch);
                }
            }
        }
    }

    private byte[] compressWithVelocity(VelocityCompressor compressor, byte[] data) {
        ByteBuf input = MoreByteBufUtils.ensureCompatible(ByteBufAllocator.DEFAULT, compressor, Unpooled.wrappedBuffer(data));
        ByteBuf output = MoreByteBufUtils.preferredBuffer(ByteBufAllocator.DEFAULT, compressor, data.length + 64);

        try {
            compressor.deflate(input, output);
            byte[] result = new byte[output.readableBytes()];
            output.readBytes(result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("VelocityCompressor compression failed", e);
        } finally {
            input.release();
            output.release();
        }
    }

    private byte[] compressWithJava(byte[] data) {
        Deflater deflater = new Deflater();
        try {
            deflater.setInput(data);
            deflater.finish();

            byte[] compressedBytes = new byte[data.length * 2];
            int compressedLength = deflater.deflate(compressedBytes);

            byte[] result = new byte[compressedLength];
            System.arraycopy(compressedBytes, 0, result, 0, compressedLength);
            return result;
        } finally {
            deflater.end();
        }
    }

    @Benchmark
    public byte[][] vanillaInflateBatch() {
        String key = dataSize.name() + "_" + dataType.name() + "_" + batchSize;
        InflateDataBatch batch = compressedDataBatches.get(key);
        byte[][] results = new byte[batchSize][];

        vanillaInflater.reset();

        for (int i = 0; i < batchSize; i++) {
            byte[] compressed = batch.vanillaCompressed[i];
            int uncompressedSize = batch.uncompressedSizes[i];

            vanillaInflater.setInput(compressed);
            byte[] result = new byte[uncompressedSize];

            try {
                int resultLength = vanillaInflater.inflate(result);
                if (resultLength != uncompressedSize) {
                    throw new RuntimeException("Decompression size mismatch");
                }
                results[i] = result;
            } catch (java.util.zip.DataFormatException e) {
                throw new RuntimeException("Decompression failed", e);
            }

            vanillaInflater.reset();
        }

        return results;
    }

    @Benchmark
    public byte[][] velocityInflateBatch() {
        String key = dataSize.name() + "_" + dataType.name() + "_" + batchSize;
        InflateDataBatch batch = compressedDataBatches.get(key);
        byte[][] results = new byte[batchSize][];

        for (int i = 0; i < batchSize; i++) {
            byte[] compressed = batch.velocityCompressed[i];
            int uncompressedSize = batch.uncompressedSizes[i];

            ByteBuf compressedBuf = MoreByteBufUtils.ensureCompatible(ByteBufAllocator.DEFAULT, velocityCompressor, Unpooled.wrappedBuffer(compressed));
            ByteBuf uncompressedBuf = MoreByteBufUtils.preferredBuffer(ByteBufAllocator.DEFAULT, velocityCompressor, uncompressedSize);

            try {
                velocityCompressor.inflate(compressedBuf, uncompressedBuf, uncompressedSize);

                byte[] result = new byte[uncompressedBuf.readableBytes()];
                uncompressedBuf.readBytes(result);
                results[i] = result;
            } catch (Exception e) {
                throw new RuntimeException("VelocityCompressor decompression failed", e);
            } finally {
                compressedBuf.release();
                uncompressedBuf.release();
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
        if (vanillaInflater != null) {
            vanillaInflater.end();
            vanillaInflater = null;
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (compressedDataBatches != null) {
            compressedDataBatches.clear();
        }
    }
}