package one.pkg.kfnp.shared.network.compression;

import com.velocitypowered.natives.util.Natives;

public class KFNPCompressorFactory {
    public static KFNPCompressor create(String protocol, int level) {
        if (protocol == null) return createDeflate(level);

        return switch (protocol.toLowerCase()) {
            case "lz4" -> new Lz4Compressor(level);
            case "zstd" -> new ZstdCompressor(level);
            case "brotli" -> new BrotliCompressor(level);
            default -> createDeflate(level);
        };
    }

    private static KFNPCompressor createDeflate(int level) {
        return new DeflateCompressor(Natives.compress.get().create(level));
    }
}
