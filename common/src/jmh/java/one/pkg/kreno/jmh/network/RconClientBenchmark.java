package one.pkg.kreno.jmh.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class RconClientBenchmark {

    private static final int CHUNK_SIZE = 4096;
    private static final int PACKET_OVERHEAD = 10;

    // Simulate the mixin field
    private final byte[] chunkBuffer = new byte[CHUNK_SIZE];

    private byte[] messageBytes;
    private ByteBufAllocator allocator;

    @Param({"100", "4096", "10000", "100000"})
    public int messageSize;

    @Setup
    public void setup() {
        allocator = ByteBufAllocator.DEFAULT;
        StringBuilder sb = new StringBuilder(messageSize);
        for (int i = 0; i < messageSize; i++) {
            sb.append('a');
        }
        messageBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Benchmark
    public void baseline(Blackhole bh) {
        int len = messageBytes.length;
        if (len <= CHUNK_SIZE) {
            send(0, 0, messageBytes, len, bh);
        } else {
            int offset = 0;
            while (offset < len) {
                int chunkSize = Math.min(CHUNK_SIZE, len - offset);
                System.arraycopy(messageBytes, offset, chunkBuffer, 0, chunkSize);
                send(0, 0, chunkBuffer, chunkSize, bh);
                offset += chunkSize;
            }
        }
    }

    @Benchmark
    public void optimized(Blackhole bh) {
        int len = messageBytes.length;
        if (len <= CHUNK_SIZE) {
            sendOptimized(0, 0, messageBytes, 0, len, bh);
        } else {
            int offset = 0;
            while (offset < len) {
                int chunkSize = Math.min(CHUNK_SIZE, len - offset);
                sendOptimized(0, 0, messageBytes, offset, chunkSize, bh);
                offset += chunkSize;
            }
        }
    }

    // Simulate the send method in RconClientMixin
    private void send(int id, int type, byte[] messageBytes, int length, Blackhole bh) {
        ByteBuf buf = allocator.buffer(length + PACKET_OVERHEAD + 4);
        try {
            buf.writeIntLE(length + PACKET_OVERHEAD);
            buf.writeIntLE(id);
            buf.writeIntLE(type);
            buf.writeBytes(messageBytes, 0, length);
            buf.writeByte(0);
            buf.writeByte(0);

            // Consume the buffer content to simulate I/O
            bh.consume(buf);
        } finally {
            buf.release();
        }
    }

    // Simulate the optimized send method
    private void sendOptimized(int id, int type, byte[] messageBytes, int offset, int length, Blackhole bh) {
        ByteBuf buf = allocator.buffer(length + PACKET_OVERHEAD + 4);
        try {
            buf.writeIntLE(length + PACKET_OVERHEAD);
            buf.writeIntLE(id);
            buf.writeIntLE(type);
            buf.writeBytes(messageBytes, offset, length); // Use offset here
            buf.writeByte(0);
            buf.writeByte(0);

            // Consume the buffer content to simulate I/O
            bh.consume(buf);
        } finally {
            buf.release();
        }
    }
}
