package one.pkg.mod.krypton_fnp.jmh.compression;

class InflateDataBatch {
    final byte[][] velocityCompressed;
    final byte[][] vanillaCompressed;
    final int[] uncompressedSizes;

    InflateDataBatch(int batchSize) {
        this.velocityCompressed = new byte[batchSize][];
        this.vanillaCompressed = new byte[batchSize][];
        this.uncompressedSizes = new int[batchSize];
    }
}
