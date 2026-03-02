package one.pkg.kfnp.shared.network.compression;

public interface ConnectionCompressorExtension {
    void kfnp$setCompressor(String compressor);
    String kfnp$getCompressor();
    void kfnp$setPeerSupportsSmartReplay(boolean supports);
    boolean kfnp$peerSupportsSmartReplay();
}
