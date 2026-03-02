package one.pkg.kfnp.shared.network.compression;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.ArrayList;
import java.util.List;

public class SmartReplayEncoder extends MessageToByteEncoder<ByteBuf> {

    private final List<ByteBuf> queue = new ArrayList<>();
    private ByteBuf lastPacket = null;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        // Only trigger on actual uncompressed minecraft packet payloads
        // A generic solution using byte arrays as identity isn't fully safe without escaping.
        // We will prepend a magic header and escape it if necessary.
        // Magic header: 0x4B 0x46 0x4E 0x50 ("KFNP")

        if (msg.readableBytes() < 2) {
            writeNormal(msg, out);
            return;
        }

        // Simplistic approach for duplicate packet queueing
        if (lastPacket != null && lastPacket.readableBytes() == msg.readableBytes() && lastPacket.equals(msg) && queue.size() < 255) {
            queue.add(msg.retain());
        } else {
            flushQueue(ctx, out);

            // Check if this packet happens to start with our magic sequence to escape it
            if (isMagic(msg)) {
                 out.writeByte(0x4B);
                 out.writeByte(0x46);
                 out.writeByte(0x4E);
                 out.writeByte(0x51); // escaped KFNQ
                 out.writeBytes(msg, msg.readerIndex() + 4, msg.readableBytes() - 4);
                 return;
            }

            queue.add(msg.retain());
            lastPacket = msg.retain();
        }
    }

    private boolean isMagic(ByteBuf buf) {
        if (buf.readableBytes() < 4) return false;
        int i = buf.readerIndex();
        return buf.getByte(i) == 0x4B && buf.getByte(i+1) == 0x46 && buf.getByte(i+2) == 0x4E && (buf.getByte(i+3) == 0x50 || buf.getByte(i+3) == 0x51);
    }

    private void writeNormal(ByteBuf msg, ByteBuf out) {
        if (isMagic(msg)) {
             out.writeByte(0x4B);
             out.writeByte(0x46);
             out.writeByte(0x4E);
             out.writeByte(0x51); // escaped
             out.writeBytes(msg, msg.readerIndex() + 4, msg.readableBytes() - 4);
        } else {
             out.writeBytes(msg);
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        if (!queue.isEmpty()) {
            ByteBuf out = ctx.alloc().buffer();
            flushQueue(ctx, out);
            if (out.isReadable()) {
                ctx.writeAndFlush(out);
            } else {
                out.release();
            }
        }
        super.flush(ctx);
    }

    private void flushQueue(ChannelHandlerContext ctx, ByteBuf out) {
        if (queue.isEmpty()) return;

        if (queue.size() == 1) {
            writeNormal(queue.get(0), out);
            queue.get(0).release();
            queue.clear();
            return;
        }

        // Custom packet format for grouped replay:
        // [4 byte magic KFNP] [1 byte count] [inner packet data]
        out.writeByte(0x4B);
        out.writeByte(0x46);
        out.writeByte(0x4E);
        out.writeByte(0x50);
        out.writeByte((byte) queue.size());
        out.writeBytes(queue.get(0));

        for (ByteBuf buf : queue) {
            buf.release();
        }
        queue.clear();

        if (lastPacket != null) {
            lastPacket.release();
            lastPacket = null;
        }
    }
}
