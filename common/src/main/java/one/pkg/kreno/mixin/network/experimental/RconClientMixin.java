package one.pkg.kreno.mixin.network.experimental;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.server.rcon.thread.RconClient;
import org.spongepowered.asm.mixin.*;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Mixin(RconClient.class)
public class RconClientMixin {
    @Unique
    private static final int CHUNK_SIZE = 4096;
    @Unique
    private static final int PACKET_OVERHEAD = 10;

    @Shadow
    @Final
    private Socket client;

    @Shadow
    private void closeSocket() {
    }

    /**
     * @author KryptonFNP
     * @reason Optimize send method to accept byte[] directly, reducing string conversions and allocations.
     */
    @Overwrite
    private void send(int id, int type, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        this.send(id, type, bytes, 0, bytes.length);
    }

    @Unique
    private void send(int id, int type, byte[] messageBytes, int offset, int length) throws IOException {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(length + PACKET_OVERHEAD + 4);
        try {
            buf.writeIntLE(length + PACKET_OVERHEAD);
            buf.writeIntLE(id);
            buf.writeIntLE(type);
            buf.writeBytes(messageBytes, offset, length);
            buf.writeByte(0);
            buf.writeByte(0);

            if (buf.hasArray()) {
                this.client.getOutputStream().write(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes());
            } else {
                buf.readBytes(this.client.getOutputStream(), buf.readableBytes());
            }
        } finally {
            buf.release();
        }
    }

    /**
     * @author KryptonFNP
     * @reason Optimize sendCmdResponse to split on byte boundaries instead of characters, reducing allocations and fixing potential encoding issues.
     */
    @Overwrite
    private void sendCmdResponse(int id, String message) throws IOException {
        byte[] fullBytes = message.getBytes(StandardCharsets.UTF_8);
        int len = fullBytes.length;

        if (len <= CHUNK_SIZE) {
            this.send(id, 0, fullBytes, 0, len);
        } else {
            int offset = 0;
            while (offset < len) {
                int chunkSize = Math.min(CHUNK_SIZE, len - offset);
                this.send(id, 0, fullBytes, offset, chunkSize);
                offset += chunkSize;
            }
        }
    }
}