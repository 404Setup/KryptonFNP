package one.pkg.mod.krypton_fnp.jmh.other;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class RconClientBenchmark {

    private static final int CHUNK_SIZE = 4096;
    private static final int PACKET_OVERHEAD = 10;

    private String smallMessage;
    private String mediumMessage;
    private String largeMessage;
    private String extraLargeMessage;

    private byte[] smallMessageBytes;
    private byte[] mediumMessageBytes;
    private byte[] largeMessageBytes;
    private byte[] extraLargeMessageBytes;

    private byte[] chunkBuffer;
    private ByteArrayOutputStream outputStream;

    @Setup
    public void setup() {
        smallMessage = "This is a test message for Rcon. ".repeat(100);
        mediumMessage = "This is a test message for Rcon. ".repeat(120);
        largeMessage = "This is a test message for Rcon. ".repeat(500);
        extraLargeMessage = "This is a test message for Rcon. ".repeat(2000);

        smallMessageBytes = smallMessage.getBytes(StandardCharsets.UTF_8);
        mediumMessageBytes = mediumMessage.getBytes(StandardCharsets.UTF_8);
        largeMessageBytes = largeMessage.getBytes(StandardCharsets.UTF_8);
        extraLargeMessageBytes = extraLargeMessage.getBytes(StandardCharsets.UTF_8);

        chunkBuffer = new byte[CHUNK_SIZE];
        outputStream = new ByteArrayOutputStream(8192);
    }

    @TearDown(Level.Invocation)
    public void cleanup() {
        outputStream.reset();
    }

    @Benchmark
    public void original_Small() throws IOException {
        originalSendCmdResponse(1, smallMessage);
    }

    @Benchmark
    public void original_Medium() throws IOException {
        originalSendCmdResponse(1, mediumMessage);
    }

    @Benchmark
    public void original_Large() throws IOException {
        originalSendCmdResponse(1, largeMessage);
    }

    @Benchmark
    public void original_ExtraLarge() throws IOException {
        originalSendCmdResponse(1, extraLargeMessage);
    }

    private void originalSendCmdResponse(int id, String message) throws IOException {
        int i = message.length();
        do {
            int j = Math.min(4096, i);
            String sub = message.substring(0, j);
            originalSend(id, 0, sub);
            message = message.substring(j);
            i = message.length();
        } while (i != 0);
    }

    private void originalSend(int id, int type, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(1248);
        buf.writeInt(Integer.reverseBytes(bytes.length + 10));
        buf.writeInt(Integer.reverseBytes(id));
        buf.writeInt(Integer.reverseBytes(type));
        buf.writeBytes(bytes);
        buf.writeByte(0);
        buf.writeByte(0);
        outputStream.write(buf.array(), buf.arrayOffset(), buf.readableBytes());
        buf.release();
    }

    @Benchmark
    public void oldOptimized_Small() throws IOException {
        oldOptimizedSendCmdResponse(1, smallMessage);
    }

    @Benchmark
    public void oldOptimized_Medium() throws IOException {
        oldOptimizedSendCmdResponse(1, mediumMessage);
    }

    @Benchmark
    public void oldOptimized_Large() throws IOException {
        oldOptimizedSendCmdResponse(1, largeMessage);
    }

    @Benchmark
    public void oldOptimized_ExtraLarge() throws IOException {
        oldOptimizedSendCmdResponse(1, extraLargeMessage);
    }

    private void oldOptimizedSendCmdResponse(int id, String message) throws IOException {
        if (message.length() < 4096) {
            String msg = message;
            int i = msg.length();
            do {
                int j = Math.min(4096, i);
                oldOptimizedSend(id, 0, msg.substring(0, j));
                msg = msg.substring(j);
                i = msg.length();
            } while (0 != i);
        } else {
            byte[] fullBytes = message.getBytes(StandardCharsets.UTF_8);
            int len = fullBytes.length;
            int offset = 0;
            while (offset < len) {
                int chunkSize = Math.min(4096, len - offset);
                System.arraycopy(fullBytes, offset, chunkBuffer, 0, chunkSize);
                oldOptimizedSendBytes(id, 0, chunkBuffer, chunkSize);
                offset += chunkSize;
            }
        }
    }

    private void oldOptimizedSend(int id, int type, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        oldOptimizedSendBytes(id, type, bytes, bytes.length);
    }

    private void oldOptimizedSendBytes(int id, int type, byte[] messageBytes, int length) throws IOException {
        ByteBuf buf = Unpooled.buffer(1248);
        buf.writeInt(Integer.reverseBytes(length + 10));
        buf.writeInt(Integer.reverseBytes(id));
        buf.writeInt(Integer.reverseBytes(type));
        buf.writeBytes(messageBytes, 0, length);
        buf.writeByte(0);
        buf.writeByte(0);
        outputStream.write(buf.array(), buf.arrayOffset(), buf.readableBytes());
        buf.release();
    }

    @Benchmark
    public void newOptimized_Small() throws IOException {
        newOptimizedSendCmdResponse(1, smallMessage);
    }

    @Benchmark
    public void newOptimized_Medium() throws IOException {
        newOptimizedSendCmdResponse(1, mediumMessage);
    }

    @Benchmark
    public void newOptimized_Large() throws IOException {
        newOptimizedSendCmdResponse(1, largeMessage);
    }

    @Benchmark
    public void newOptimized_ExtraLarge() throws IOException {
        newOptimizedSendCmdResponse(1, extraLargeMessage);
    }

    private void newOptimizedSendCmdResponse(int id, String message) throws IOException {
        byte[] fullBytes = message.getBytes(StandardCharsets.UTF_8);
        int len = fullBytes.length;

        if (len <= CHUNK_SIZE) {
            newOptimizedSend(id, 0, fullBytes, len);
        } else {
            int offset = 0;
            while (offset < len) {
                int chunkSize = Math.min(CHUNK_SIZE, len - offset);
                System.arraycopy(fullBytes, offset, chunkBuffer, 0, chunkSize);
                newOptimizedSend(id, 0, chunkBuffer, chunkSize);
                offset += chunkSize;
            }
        }
    }

    private void newOptimizedSend(int id, int type, byte[] messageBytes, int length) throws IOException {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(length + PACKET_OVERHEAD + 2);
        try {
            buf.writeIntLE(length + PACKET_OVERHEAD);
            buf.writeIntLE(id);
            buf.writeIntLE(type);
            buf.writeBytes(messageBytes, 0, length);
            buf.writeByte(0);
            buf.writeByte(0);

            if (buf.hasArray()) {
                outputStream.write(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes());
            } else {
                byte[] temp = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), temp);
                outputStream.write(temp);
            }
        } finally {
            buf.release();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void allocationTest_Unpooled() {
        ByteBuf buf = Unpooled.buffer(1024);
        buf.writeInt(123);
        buf.release();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void allocationTest_Pooled() {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(1024);
        buf.writeInt(123);
        buf.release();
    }

    @Benchmark
    public void preConverted_Small() throws IOException {
        sendPreConverted(1, smallMessageBytes);
    }

    @Benchmark
    public void preConverted_Large() throws IOException {
        sendPreConverted(1, largeMessageBytes);
    }

    private void sendPreConverted(int id, byte[] fullBytes) throws IOException {
        int len = fullBytes.length;
        if (len <= CHUNK_SIZE) {
            newOptimizedSend(id, 0, fullBytes, len);
        } else {
            int offset = 0;
            while (offset < len) {
                int chunkSize = Math.min(CHUNK_SIZE, len - offset);
                System.arraycopy(fullBytes, offset, chunkBuffer, 0, chunkSize);
                newOptimizedSend(id, 0, chunkBuffer, chunkSize);
                offset += chunkSize;
            }
        }
    }
}