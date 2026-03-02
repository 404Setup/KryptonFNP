package one.pkg.kfnp.shared.network.compression;

import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;

public interface KryptonCompressor extends AutoCloseable {
    void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize) throws DataFormatException;
    void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException;

    @Override
    default void close() {
    }
}
