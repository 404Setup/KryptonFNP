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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Deflate {

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
    private java.util.zip.Deflater vanillaDeflater;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        random = new Random(42);

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
        vanillaDeflater = new java.util.zip.Deflater();
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
        String pattern = "Player" + (index % 10) + "_position_update_data_";
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
        int repetitiveSize = (int) (size * 0.6);

        String pattern = "MixedData_Pattern_" + (index % 3) + "_";
        byte[] patternBytes = pattern.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < repetitiveSize; i++) {
            data[i] = patternBytes[i % patternBytes.length];
        }

        for (int i = repetitiveSize; i < size; i++) {
            data[i] = (byte) random.nextInt(256);
        }

        return data;
    }

    private byte[] generateMinecraftLikeData(int size, int index) {
        ByteBuf buf = Unpooled.buffer(size);

        try {
            switch (index % 5) {
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
                case 4:
                    generateInventoryPacket(buf, size);
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
            writeVarInt(buf, random.nextInt(100));  // Block ID
        }
    }

    private void generateEntityMovePacket(ByteBuf buf, int targetSize) {
        while (buf.readableBytes() < targetSize - 50) {
            writeVarInt(buf, random.nextInt(1000)); // Entity ID
            buf.writeDouble(random.nextDouble() * 100); // X
            buf.writeDouble(random.nextDouble() * 100); // Y
            buf.writeDouble(random.nextDouble() * 100); // Z
            buf.writeFloat(random.nextFloat() * 360);   // Yaw
            buf.writeFloat(random.nextFloat() * 180);   // Pitch
        }
    }

    private void generateChatPacket(ByteBuf buf, int targetSize) {
        String[] messages = {
                "Hello world!",
                "How are you doing?",
                "Good morning everyone!",
                "See you later!",
                "Thanks for the help!",
                "Great game!",
                "Nice build!",
                "Where are you?",
                "Come here!",
                "Follow me!"
        };

        while (buf.readableBytes() < targetSize - 100) {
            String msg = messages[random.nextInt(messages.length)];
            writeString(buf, msg);
            buf.writeLong(System.currentTimeMillis());
        }
    }

    private void generateChunkDataPacket(ByteBuf buf, int targetSize) {
        int[] commonBlocks = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        while (buf.readableBytes() < targetSize - 10) {
            if (random.nextFloat() < 0.9f) {
                writeVarInt(buf, commonBlocks[random.nextInt(commonBlocks.length)]);
            } else {
                writeVarInt(buf, random.nextInt(1000));
            }
        }
    }

    private void generateInventoryPacket(ByteBuf buf, int targetSize) {
        int[] commonItems = {1, 2, 3, 4, 5, 64, 65, 66, 67, 68};

        while (buf.readableBytes() < targetSize - 20) {
            writeVarInt(buf, random.nextInt(36));
            if (random.nextFloat() < 0.7f) {
                writeVarInt(buf, commonItems[random.nextInt(commonItems.length)]);
                buf.writeByte(random.nextInt(64) + 1);
            } else {
                writeVarInt(buf, 0);
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

    private static class TestDataBatch {
        final byte[][] testData;
        final boolean[] shouldCompress;

        TestDataBatch(int batchSize) {
            this.testData = new byte[batchSize][];
            this.shouldCompress = new boolean[batchSize];
        }
    }
}