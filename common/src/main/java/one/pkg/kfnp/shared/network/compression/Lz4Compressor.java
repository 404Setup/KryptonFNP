package one.pkg.kfnp.shared.network.compression;

import io.netty.buffer.ByteBuf;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

public class Lz4Compressor implements KFNPCompressor {
    private final LZ4Compressor compressor;
    private final LZ4FastDecompressor decompressor;

    public Lz4Compressor(int level) {
        LZ4Factory factory = LZ4Factory.fastestInstance();
        this.compressor = level > 5 ? factory.highCompressor() : factory.fastCompressor();
        this.decompressor = factory.fastDecompressor();
    }

    @Override
    public void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize) throws DataFormatException {
        ByteBuffer in = source.nioBuffer();
        ByteBuffer out = destination.nioBuffer(destination.writerIndex(), destination.writableBytes());
        try {
            int decompressedBytes = decompressor.decompress(in, in.position(), out, out.position(), uncompressedSize);
            destination.writerIndex(destination.writerIndex() + uncompressedSize);
            source.readerIndex(source.readerIndex() + decompressedBytes);
        } catch (Exception e) {
            throw new DataFormatException("LZ4 Decompression failed: " + e.getMessage());
        }
    }

    @Override
    public void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException {
        int uncompressedSize = source.readableBytes();
        int maxCompressedLength = compressor.maxCompressedLength(uncompressedSize);
        destination.ensureWritable(maxCompressedLength);

        ByteBuffer in = source.nioBuffer();
        ByteBuffer out = destination.nioBuffer(destination.writerIndex(), destination.writableBytes());

        try {
            int compressedBytes = compressor.compress(in, in.position(), uncompressedSize, out, out.position(), maxCompressedLength);
            destination.writerIndex(destination.writerIndex() + compressedBytes);
            source.readerIndex(source.readerIndex() + uncompressedSize);
        } catch (Exception e) {
            throw new DataFormatException("LZ4 Compression failed: " + e.getMessage());
        }
    }
}
