package me.steinborn.krypton.jmh.compression;

public enum DataSize {
    SMALL(128),      // 128 bytes
    MEDIUM(1024),    // 1KB
    LARGE(8192),     // 8KB
    XLARGE(32768);   // 32KB

    private final int bytes;

    DataSize(int bytes) {
        this.bytes = bytes;
    }

    public int getBytes() {
        return bytes;
    }
}