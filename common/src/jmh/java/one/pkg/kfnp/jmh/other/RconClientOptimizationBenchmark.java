package one.pkg.kfnp.jmh.other;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.openjdk.jmh.annotations.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class RconClientOptimizationBenchmark {

    private static final int CHUNK_SIZE = 4096;
    private static final int PACKET_OVERHEAD = 10;

    private byte[] smallMessageBytes;
    private byte[] largeMessageBytes;

    private byte[] chunkBuffer;
    private ByteArrayOutputStream outputStream;

    @Setup
    public void setup() {
        String smallMessage = "This is a test message for Rcon. ".repeat(10);
        String largeMessage = "This is a test message for Rcon. ".repeat(500);

        smallMessageBytes = smallMessage.getBytes(StandardCharsets.UTF_8);
        largeMessageBytes = largeMessage.getBytes(StandardCharsets.UTF_8);

        chunkBuffer = new byte[CHUNK_SIZE];
        outputStream = new ByteArrayOutputStream(8192);
    }

    @TearDown(Level.Invocation)
    public void cleanup() {
        outputStream.reset();
    }

    @Benchmark
    public void original_Small_Direct() throws IOException {
        originalSend(1, 0, smallMessageBytes, smallMessageBytes.length, true);
    }

    @Benchmark
    public void original_Large_Direct() throws IOException {
        originalSend(1, 0, largeMessageBytes, largeMessageBytes.length, true);
    }

    @Benchmark
    public void readBytes_Small_Direct() throws IOException {
        readBytesSend(1, 0, smallMessageBytes, smallMessageBytes.length, true);
    }

    @Benchmark
    public void readBytes_Large_Direct() throws IOException {
        readBytesSend(1, 0, largeMessageBytes, largeMessageBytes.length, true);
    }

    @Benchmark
    public void reusedBuffer_Small_Direct() throws IOException {
        reusedBufferSend(1, 0, smallMessageBytes, smallMessageBytes.length, true);
    }

    @Benchmark
    public void reusedBuffer_Large_Direct() throws IOException {
        reusedBufferSend(1, 0, largeMessageBytes, largeMessageBytes.length, true);
    }

    private void originalSend(int id, int type, byte[] messageBytes, int length, boolean direct) throws IOException {
        ByteBuf buf = direct ? ByteBufAllocator.DEFAULT.directBuffer(length + PACKET_OVERHEAD + 2) : ByteBufAllocator.DEFAULT.heapBuffer(length + PACKET_OVERHEAD + 2);
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

    private void readBytesSend(int id, int type, byte[] messageBytes, int length, boolean direct) throws IOException {
        ByteBuf buf = direct ? ByteBufAllocator.DEFAULT.directBuffer(length + PACKET_OVERHEAD + 2) : ByteBufAllocator.DEFAULT.heapBuffer(length + PACKET_OVERHEAD + 2);
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
                buf.readBytes(outputStream, buf.readableBytes());
            }
        } finally {
            buf.release();
        }
    }

    private void reusedBufferSend(int id, int type, byte[] messageBytes, int length, boolean direct) throws IOException {
        ByteBuf buf = direct ? ByteBufAllocator.DEFAULT.directBuffer(length + PACKET_OVERHEAD + 2) : ByteBufAllocator.DEFAULT.heapBuffer(length + PACKET_OVERHEAD + 2);
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
                int len = buf.readableBytes();
                while (len > 0) {
                     int toRead = Math.min(len, chunkBuffer.length);
                     buf.readBytes(chunkBuffer, 0, toRead);
                     outputStream.write(chunkBuffer, 0, toRead);
                     len -= toRead;
                }
            }
        } finally {
            buf.release();
        }
    }
}
