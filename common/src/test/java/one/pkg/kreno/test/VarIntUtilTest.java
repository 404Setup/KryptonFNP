package one.pkg.kreno.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import one.pkg.kreno.shared.network.util.VarIntUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VarIntUtilTest {

    @Test
    public void testReadWriteVarInt() {
        testVarInt(0, 1);
        testVarInt(1, 1);
        testVarInt(127, 1);
        testVarInt(128, 2);
        testVarInt(255, 2);
        testVarInt(16383, 2); // 14 bits
        testVarInt(16384, 3);
        testVarInt(2097151, 3); // 21 bits
        testVarInt(2097152, 4);
        testVarInt(268435455, 4); // 28 bits
        testVarInt(268435456, 5);
        testVarInt(-1, 5);
        testVarInt(Integer.MAX_VALUE, 5);
        testVarInt(Integer.MIN_VALUE, 5);
    }

    private void testVarInt(int value, int expectedBytes) {
        ByteBuf buf = Unpooled.buffer();
        VarIntUtil.writeVarInt(buf, value);

        assertEquals(expectedBytes, buf.readableBytes(), "Byte length mismatch for value: " + value);
        assertEquals(expectedBytes, VarIntUtil.getVarIntLength(value), "VarIntUtil.getVarIntLength mismatch for value: " + value);

        int read = VarIntUtil.readVarInt(buf);
        assertEquals(value, read, "Read value mismatch");
        assertEquals(0, buf.readableBytes(), "Buffer should be empty after read");
        buf.release();
    }
}
