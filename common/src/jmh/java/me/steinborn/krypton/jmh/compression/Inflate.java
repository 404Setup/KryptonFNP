package me.steinborn.krypton.jmh.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Inflate {

    @Param({"SMALL", "MEDIUM", "LARGE"})
    private DataSize dataSize;

    @Param({"REPETITIVE", "MINECRAFT_LIKE"})
    private DataType dataType;

    @Param({"15", "75"})
    private int batchSize;

    private Map<String, CompressedDataBatch> compressedDataBatches;
    private VelocityCompressor velocityCompressor;
    private java.util.zip.Inflater vanillaInflater;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        random = new Random(42);

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
        vanillaInflater = new java.util.zip.Inflater();
    }

    private void prepareCompressedDataBatches() {
        try (VelocityCompressor tempCompressor = Natives.compress.get().create(4)) {
            for (DataSize size : DataSize.values()) {
                for (DataType type : DataType.values()) {
                    if (type == DataType.RANDOM || type == DataType.MIXED) {
                        continue;
                    }

                    String key = size.name() + "_" + type.name() + "_" + batchSize;
                    CompressedDataBatch batch = new CompressedDataBatch(batchSize);

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
        ByteBuf input = ensureCompatible(ByteBufAllocator.DEFAULT, compressor, Unpooled.wrappedBuffer(data));
        ByteBuf output = preferredBuffer(ByteBufAllocator.DEFAULT, compressor, data.length + 64);

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

    private byte[] generateTestData(DataSize size, DataType type, int index) {
        int targetSize = size.getBytes();

        return switch (type) {
            case REPETITIVE -> generateRepetitiveData(targetSize, index);
            case RANDOM -> generateRandomData(targetSize);
            case MIXED -> generateMixedData(targetSize, index);
            case MINECRAFT_LIKE -> generateMinecraftLikeData(targetSize, index);
        };
    }

    private byte[] generateRepetitiveData(int size, int index) {
        StringBuilder sb = new StringBuilder();
        String pattern = "Player" + (index % 10) + "_position_update_";
        while (sb.length() < size) {
            sb.append(pattern);
        }
        return sb.substring(0, size).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateRandomData(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }

    private byte[] generateMixedData(int size, int index) {
        byte[] data = new byte[size];
        int half = size / 2;

        String pattern = "MixedData_" + (index % 5) + "_";
        byte[] patternBytes = pattern.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < half; i++) {
            data[i] = patternBytes[i % patternBytes.length];
        }

        for (int i = half; i < size; i++) {
            data[i] = (byte) random.nextInt(256);
        }

        return data;
    }

    private byte[] generateMinecraftLikeData(int size, int index) {
        ByteBuf buf = Unpooled.buffer(size);

        try {
            switch (index % 4) {
                case 0:
                    generateBlockUpdatePacket(buf, size);
                    break;
                case 1:
                    generateEntityMovePacket(buf, size);
                    break;
                case 2:
                    generateChatPacket(buf, size);
                    break;
                case 3:
                    generateChunkDataPacket(buf, size);
                    break;
            }

            while (buf.readableBytes() < size) {
                buf.writeByte(0);
            }

            byte[] result = new byte[Math.min(size, buf.readableBytes())];
            buf.readBytes(result);
            return result;
        } finally {
            buf.release();
        }
    }

    private void generateBlockUpdatePacket(ByteBuf buf, int targetSize) {
        while (buf.readableBytes() < targetSize - 20) {
            writeVarInt(buf, random.nextInt(1000)); // X
            writeVarInt(buf, random.nextInt(256));  // Y
            writeVarInt(buf, random.nextInt(1000)); // Z
            writeVarInt(buf, random.nextInt(1000)); // Block ID
        }
    }

    private void generateEntityMovePacket(ByteBuf buf, int targetSize) {
        while (buf.readableBytes() < targetSize - 50) {
            writeVarInt(buf, random.nextInt(10000)); // Entity ID
            buf.writeDouble(random.nextDouble() * 1000); // X
            buf.writeDouble(random.nextDouble() * 256);  // Y
            buf.writeDouble(random.nextDouble() * 1000); // Z
            buf.writeFloat(random.nextFloat() * 360);    // Yaw
            buf.writeFloat(random.nextFloat() * 360);    // Pitch
        }
    }

    private void generateChatPacket(ByteBuf buf, int targetSize) {
        String[] messages = {
                "Hello world!",
                "How are you?",
                "Good morning!",
                "See you later!",
                "Thanks!"
        };

        while (buf.readableBytes() < targetSize - 100) {
            String msg = messages[random.nextInt(messages.length)];
            writeString(buf, msg);
        }
    }

    private void generateChunkDataPacket(ByteBuf buf, int targetSize) {
        int[] commonBlocks = {0, 1, 2, 3, 4, 5};

        while (buf.readableBytes() < targetSize - 10) {
            if (random.nextFloat() < 0.8f) {
                writeVarInt(buf, commonBlocks[random.nextInt(commonBlocks.length)]);
            } else {
                writeVarInt(buf, random.nextInt(1000));
            }
        }
    }

    private void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    private void writeString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    @Benchmark
    public byte[][] vanillaInflateBatch() {
        String key = dataSize.name() + "_" + dataType.name() + "_" + batchSize;
        CompressedDataBatch batch = compressedDataBatches.get(key);
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
        CompressedDataBatch batch = compressedDataBatches.get(key);
        byte[][] results = new byte[batchSize][];

        for (int i = 0; i < batchSize; i++) {
            byte[] compressed = batch.velocityCompressed[i];
            int uncompressedSize = batch.uncompressedSizes[i];

            ByteBuf compressedBuf = ensureCompatible(ByteBufAllocator.DEFAULT, velocityCompressor, Unpooled.wrappedBuffer(compressed));
            ByteBuf uncompressedBuf = preferredBuffer(ByteBufAllocator.DEFAULT, velocityCompressor, uncompressedSize);

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

    public enum DataSize {
        SMALL(128),      // 128 bytes
        MEDIUM(1024),    // 1KB
        LARGE(8192),     // 8KB
        XLARGE(32768);   // 32KB

        private final int bytes;

        DataSize(int bytes) {
            this.bytes = bytes;
        }

        public int getBytes() {
            return bytes;
        }
    }

    public enum DataType {
        REPETITIVE,
        RANDOM,
        MIXED,
        MINECRAFT_LIKE
    }

    // 存储压缩数据的结构
    private static class CompressedDataBatch {
        final byte[][] velocityCompressed;
        final byte[][] vanillaCompressed;
        final int[] uncompressedSizes;

        CompressedDataBatch(int batchSize) {
            this.velocityCompressed = new byte[batchSize][];
            this.vanillaCompressed = new byte[batchSize][];
            this.uncompressedSizes = new int[batchSize];
        }
    }
}