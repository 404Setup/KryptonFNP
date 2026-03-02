package one.pkg.kfnp.shared.network.compression;

import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

public class ZstdCompressor implements KryptonCompressor {
    private final int level;

    public ZstdCompressor(int level) {
        this.level = level;
    }

    @Override
    public void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize) throws DataFormatException {
        ByteBuffer in = source.nioBuffer();
        ByteBuffer out = destination.nioBuffer(destination.writerIndex(), destination.writableBytes());

        try {
            int decompressedBytes = Zstd.decompress(out, in);
            if (decompressedBytes != uncompressedSize) {
                throw new DataFormatException("Zstd decompression size mismatch");
            }
            destination.writerIndex(destination.writerIndex() + decompressedBytes);
            source.readerIndex(source.readerIndex() + in.position());
        } catch (Exception e) {
            throw new DataFormatException("Zstd Decompression failed: " + e.getMessage());
        }
    }

    @Override
    public void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException {
        int uncompressedSize = source.readableBytes();
        int maxCompressedLength = (int) Zstd.compressBound(uncompressedSize);
        destination.ensureWritable(maxCompressedLength);

        ByteBuffer in = source.nioBuffer();
        ByteBuffer out = destination.nioBuffer(destination.writerIndex(), destination.writableBytes());

        try {
            int compressedBytes = Zstd.compress(out, in, level);
            destination.writerIndex(destination.writerIndex() + compressedBytes);
            source.readerIndex(source.readerIndex() + uncompressedSize);
        } catch (Exception e) {
            throw new DataFormatException("Zstd Compression failed: " + e.getMessage());
        }
    }
}
