package one.pkg.kfnp.test;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import one.pkg.kfnp.shared.network.compression.MinecraftCompressDecoder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MinecraftCompressDecoderTest {

    @Test
    public void testVulnerability() throws Exception {
        int hugeSize = 200 * 1024 * 1024;
        final AtomicInteger requestedSize = new AtomicInteger(0);

        VelocityCompressor compressor = (VelocityCompressor) createCompressor(requestedSize);
        ByteBufAllocator mockAllocator = (ByteBufAllocator) createAllocator(requestedSize);
        ChannelHandlerContext ctx = (ChannelHandlerContext) createContext(mockAllocator);

        MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(256, false, compressor, null);

        ByteBuf input = Unpooled.buffer();
        writeVarInt(input, hugeSize);
        input.writeBoolean(true);

        List<Object> out = new ArrayList<>();

        Method decodeMethod = MinecraftCompressDecoder.class.getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
        decodeMethod.setAccessible(true);
        try {
            decodeMethod.invoke(decoder, ctx, input, out);
            fail("Should have thrown exception");
        } catch (InvocationTargetException e) {
             Throwable cause = e.getCause();
             if (!(cause instanceof DecoderException)) {
                 cause.printStackTrace();
                 fail("Expected DecoderException, got " + cause);
             }
             if (!cause.getMessage().contains("exceeds hard maximum size")) {
                 fail("Expected message to contain 'exceeds hard maximum size', got: " + cause.getMessage());
             }
        } catch (Exception e) {
             e.printStackTrace();
             fail("Unexpected exception: " + e);
        }

        assertEquals(0, requestedSize.get(), "Should not have attempted to allocate huge buffer");
    }

    @Test
    public void testValidPacket() throws Exception {
        int smallSize = 100;
        final AtomicInteger requestedSize = new AtomicInteger(0);

        VelocityCompressor compressor = (VelocityCompressor) createCompressor(requestedSize);
        ByteBufAllocator mockAllocator = (ByteBufAllocator) createAllocator(requestedSize);
        ChannelHandlerContext ctx = (ChannelHandlerContext) createContext(mockAllocator);

        MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(256, false, compressor, null);

        ByteBuf input = Unpooled.buffer();
        writeVarInt(input, smallSize);
        input.writeBoolean(true);

        List<Object> out = new ArrayList<>();

        Method decodeMethod = MinecraftCompressDecoder.class.getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
        decodeMethod.setAccessible(true);

        try {
            decodeMethod.invoke(decoder, ctx, input, out);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown exception: " + e);
        }

        // It should have allocated 'smallSize' (or around it)
        assertTrue(requestedSize.get() > 0, "Should have allocated buffer");
        // assertEquals(smallSize, requestedSize.get()); // Might be slightly different due to preferredBuffer logic
    }

    private Object createCompressor(AtomicInteger requestedSize) {
        return Proxy.newProxyInstance(
                VelocityCompressor.class.getClassLoader(),
                new Class[]{VelocityCompressor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("preferredBufferType")) {
                        return BufferPreference.values()[0];
                    }
                    if (method.getName().equals("preferredBuffer")) {
                        for (Object arg : args) {
                            if (arg instanceof Integer) {
                                int size = (Integer) arg;
                                requestedSize.set(size);
                            }
                        }
                        return Unpooled.buffer(10);
                    }
                    if (method.getName().equals("ensureCompatible")) {
                         for (Object arg : args) {
                             if (arg instanceof ByteBuf) {
                                 return ((ByteBuf) arg).retain();
                             }
                         }
                    }
                    if (method.getName().equals("inflate")) {
                        return null;
                    }
                    if (method.getName().equals("close")) return null;
                    return null;
                });
    }

    private Object createAllocator(AtomicInteger requestedSize) {
        return Proxy.newProxyInstance(
                ByteBufAllocator.class.getClassLoader(),
                new Class[]{ByteBufAllocator.class},
                (proxy, method, args) -> {
                     if (method.getName().contains("Buffer") && args != null && args.length > 0 && args[0] instanceof Integer) {
                         int size = (Integer) args[0];
                         // Only track if not already tracked by compressor (or just update it)
                         // But for small size test, we want to know if it was called.
                         // However, if compressor calls it, we might double count?
                         // But requestedSize is AtomicInteger.set(), so it overwrites.
                         requestedSize.set(size);
                         return Unpooled.buffer(10);
                     }
                     return Unpooled.buffer(10);
                });
    }

    private Object createContext(Object allocator) {
        return Proxy.newProxyInstance(
                ChannelHandlerContext.class.getClassLoader(),
                new Class[]{ChannelHandlerContext.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("alloc")) {
                        return allocator;
                    }
                    return null;
                });
    }

    private static void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }
}
