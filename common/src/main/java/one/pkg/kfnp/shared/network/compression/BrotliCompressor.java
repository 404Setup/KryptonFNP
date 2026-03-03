package one.pkg.kfnp.shared.network.compression;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.Decoder;
import com.aayushatharva.brotli4j.encoder.Encoder;
import io.netty.buffer.ByteBuf;

import java.util.zip.DataFormatException;

public class BrotliCompressor implements KFNPCompressor {
    static {
        Brotli4jLoader.ensureAvailability();
    }

    private final int level;

    public BrotliCompressor(int level) {
        this.level = level;
    }

    @Override
    public void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize) throws DataFormatException {
        byte[] compressedData = new byte[source.readableBytes()];
        source.readBytes(compressedData);

        try {
            byte[] decompressedData = Decoder.decompress(compressedData).getDecompressedData();
            if (decompressedData.length != uncompressedSize) {
                throw new DataFormatException("Brotli decompression size mismatch");
            }
            destination.writeBytes(decompressedData);
        } catch (Exception e) {
            throw new DataFormatException("Brotli Decompression failed: " + e.getMessage());
        }
    }

    @Override
    public void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException {
        byte[] uncompressedData = new byte[source.readableBytes()];
        source.readBytes(uncompressedData);

        Encoder.Parameters params = new Encoder.Parameters().setQuality(level);
        try {
            byte[] compressedData = Encoder.compress(uncompressedData, params);
            destination.writeBytes(compressedData);
        } catch (Exception e) {
            throw new DataFormatException("Brotli Compression failed: " + e.getMessage());
        }
    }
}
