package one.pkg.kfnp.test;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import one.pkg.kfnp.shared.network.compression.MinecraftCompressEncoder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MinecraftCompressEncoderTest {

    @Test
    public void testAllocateBufferCalculatesCorrectSize() throws Exception {
        int inputSize = 1000;
        final AtomicInteger requestedSize = new AtomicInteger(0);

        VelocityCompressor compressor = (VelocityCompressor) createCompressor();
        ByteBufAllocator mockAllocator = (ByteBufAllocator) createAllocator(requestedSize);
        ChannelHandlerContext ctx = (ChannelHandlerContext) createContext(mockAllocator);

        MinecraftCompressEncoder encoder = new MinecraftCompressEncoder(256, compressor, null);

        ByteBuf input = Unpooled.buffer(inputSize);
        input.writeZero(inputSize); // Fill with zeros

        // Access protected method allocateBuffer
        Method allocateBufferMethod = MinecraftCompressEncoder.class.getDeclaredMethod("allocateBuffer", ChannelHandlerContext.class, ByteBuf.class, boolean.class);
        allocateBufferMethod.setAccessible(true);

        try {
            allocateBufferMethod.invoke(encoder, ctx, input, true);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }

        // Optimized behavior: inputSize + 64
        assertEquals(inputSize + 64, requestedSize.get(), "Should have allocated inputSize + 64");
    }

    private Object createCompressor() {
        return Proxy.newProxyInstance(
                VelocityCompressor.class.getClassLoader(),
                new Class[]{VelocityCompressor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("preferredBufferType")) {
                        return BufferPreference.values()[0]; // HEAP or DIRECT, doesn't matter for size check usually
                    }
                    if (method.getName().equals("preferredBuffer")) {
                         // Some implementations might call this
                        return Unpooled.buffer(10);
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
}
