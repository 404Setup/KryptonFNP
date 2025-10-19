package one.pkg.mod.krypton_fnp.shared.network;

import io.netty.util.ByteProcessor;
import one.pkg.mod.krypton_fnp.shared.ModConfig;

public class VarIntByteDecoder implements ByteProcessor {

    private static final int MAXIMUM_VARINT_BYTE_SIZE = ModConfig.Compression.isPermitOversizedPackets()
            ? 5 : 3;

    private int readVarint;
    private int bytesRead;
    private DecodeResult result = DecodeResult.TOO_SHORT;

    @Override
    public boolean process(byte k) {
        if (k == 0 && bytesRead == 0) {
            // tentatively say it's invalid, but there's a possibility of redemption
            result = DecodeResult.RUN_OF_ZEROES;
            return true;
        }
        if (result == DecodeResult.RUN_OF_ZEROES) {
            return false;
        }
        readVarint |= (k & 0x7F) << bytesRead++ * 7;
        if (bytesRead > MAXIMUM_VARINT_BYTE_SIZE) {
            result = DecodeResult.TOO_BIG;
            return false;
        }
        if ((k & 0x80) != 128) {
            result = DecodeResult.SUCCESS;
            return false;
        }
        return true;
    }

    public int getReadVarint() {
        return readVarint;
    }

    public int getBytesRead() {
        return bytesRead;
    }

    public DecodeResult getResult() {
        return result;
    }

    public void reset() {
        readVarint = 0;
        bytesRead = 0;
        result = DecodeResult.TOO_SHORT;
    }

    public enum DecodeResult {
        SUCCESS,
        TOO_SHORT,
        TOO_BIG,
        RUN_OF_ZEROES
    }
}