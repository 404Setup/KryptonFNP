package one.pkg.kfnp.shared.network.compression;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class SmartReplayDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (!msg.isReadable()) return;

        if (isMagic(msg)) {
            byte last = msg.getByte(msg.readerIndex() + 3);
            if (last == 0x51) {
                // Escaped normal packet
                ByteBuf normal = ctx.alloc().buffer(msg.readableBytes());
                normal.writeByte(0x4B);
                normal.writeByte(0x46);
                normal.writeByte(0x4E);
                normal.writeByte(0x50);
                normal.writeBytes(msg, msg.readerIndex() + 4, msg.readableBytes() - 4);
                out.add(normal);
                msg.skipBytes(msg.readableBytes());
            } else if (last == 0x50) {
                // Grouped packet
                msg.skipBytes(4);
                int count = msg.readUnsignedByte();
                ByteBuf innerPacket = msg.readBytes(msg.readableBytes());

                for (int i = 0; i < count; i++) {
                    out.add(innerPacket.retainedDuplicate());
                }
                innerPacket.release();
            }
        } else {
            out.add(msg.retain());
        }
    }

    private boolean isMagic(ByteBuf buf) {
        if (buf.readableBytes() < 4) return false;
        int i = buf.readerIndex();
        return buf.getByte(i) == 0x4B && buf.getByte(i + 1) == 0x46 && buf.getByte(i + 2) == 0x4E && (buf.getByte(i + 3) == 0x50 || buf.getByte(i + 3) == 0x51);
    }
}
