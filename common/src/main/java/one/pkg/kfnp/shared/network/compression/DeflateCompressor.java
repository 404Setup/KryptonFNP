package one.pkg.kfnp.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;

import java.util.zip.DataFormatException;

public class DeflateCompressor implements KryptonCompressor {
    private final VelocityCompressor delegate;

    public DeflateCompressor(VelocityCompressor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize) throws DataFormatException {
        delegate.inflate(source, destination, uncompressedSize);
    }

    @Override
    public void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException {
        delegate.deflate(source, destination);
    }

    @Override
    public void close() {
        delegate.close();
    }

    public VelocityCompressor getDelegate() {
        return delegate;
    }
}
