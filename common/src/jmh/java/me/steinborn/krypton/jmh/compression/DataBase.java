package me.steinborn.krypton.jmh.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class DataBase {
    Random random = new Random(42);

    void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    void writeString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    byte[] generateTestData(DataSize size, DataType type, int index) {
        int targetSize = size.getBytes();

        return switch (type) {
            case REPETITIVE -> generateRepetitiveData(targetSize, index);
            case RANDOM -> generateRandomData(targetSize);
            case MIXED -> generateMixedData(targetSize, index);
            case MINECRAFT_LIKE -> generateMinecraftLikeData(targetSize, index);
        };
    }

    byte[] generateRepetitiveData(int size, int index) {
        StringBuilder sb = new StringBuilder();
        String pattern = "Player" + (index % 10) + "_position_update_data_";
        while (sb.length() < size) {
            sb.append(pattern);
        }
        return sb.substring(0, size).getBytes(StandardCharsets.UTF_8);
    }

    byte[] generateRandomData(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }

    byte[] generateMixedData(int size, int index) {
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

    byte[] generateMinecraftLikeData(int size, int index) {
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

    void generateBlockUpdatePacket(ByteBuf buf, int targetSize) {
        while (buf.readableBytes() < targetSize - 20) {
            writeVarInt(buf, random.nextInt(1000)); // X
            writeVarInt(buf, random.nextInt(256));  // Y
            writeVarInt(buf, random.nextInt(1000)); // Z
            writeVarInt(buf, random.nextInt(100));  // Block ID
        }
    }

    void generateEntityMovePacket(ByteBuf buf, int targetSize) {
        while (buf.readableBytes() < targetSize - 50) {
            writeVarInt(buf, random.nextInt(10000)); // Entity ID
            buf.writeDouble(random.nextDouble() * 1000); // X
            buf.writeDouble(random.nextDouble() * 256); // Y
            buf.writeDouble(random.nextDouble() * 1000); // Z
            buf.writeFloat(random.nextFloat() * 360);   // Yaw
            buf.writeFloat(random.nextFloat() * 180);   // Pitch
        }
    }

    void generateChatPacket(ByteBuf buf, int targetSize) {
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

    void generateChunkDataPacket(ByteBuf buf, int targetSize) {
        int[] commonBlocks = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

        while (buf.readableBytes() < targetSize - 10) {
            if (random.nextFloat() < 0.9f) {
                writeVarInt(buf, commonBlocks[random.nextInt(commonBlocks.length)]);
            } else {
                writeVarInt(buf, random.nextInt(1000));
            }
        }
    }

    void generateInventoryPacket(ByteBuf buf, int targetSize) {
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
}
