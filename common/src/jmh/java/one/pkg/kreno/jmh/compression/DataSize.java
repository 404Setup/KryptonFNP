package one.pkg.kreno.jmh.compression;

public enum DataSize {
    SMALL(128),
    MEDIUM(1024),
    LARGE(8192),
    XLARGE(32768);

    private final int bytes;

    DataSize(int bytes) {
        this.bytes = bytes;
    }

    public int getBytes() {
        return bytes;
    }
}