package one.pkg.kfnp.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.natives.compression.JavaVelocityCompressor;

public class KryptonCompressorFactory {
    public static KryptonCompressor create(String protocol, int level) {
        if (protocol == null) return createDeflate(level);

        switch (protocol.toLowerCase()) {
            case "lz4":
                return new Lz4Compressor(level);
            case "zstd":
                return new ZstdCompressor(level);
            case "brotli":
                return new BrotliCompressor(level);
            case "deflate":
            default:
                return createDeflate(level);
        }
    }

    private static KryptonCompressor createDeflate(int level) {
        VelocityCompressor compressor = Natives.compress.get().create(level);
        return new DeflateCompressor(compressor);
    }
}
