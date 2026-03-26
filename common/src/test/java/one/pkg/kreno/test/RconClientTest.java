package one.pkg.kreno.test;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RconClientTest {

    private static int redirectIntFromByteArrayOptimized(byte[] pInput, int pOffset, int pLength) {
        if (pLength - pOffset < 4) {
            return 0;
        }
        return (pInput[pOffset + 3] & 0xFF) << 24 |
                (pInput[pOffset + 2] & 0xFF) << 16 |
                (pInput[pOffset + 1] & 0xFF) << 8 |
                (pInput[pOffset] & 0xFF);
    }

    @Test
    public void testRedirectIntFromByteArray() {
        byte[] testBytes = {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78};
        int result = redirectIntFromByteArrayOptimized(testBytes, 0, 4);
        assertEquals(0x78563412, result);
    }

    @Test
    public void testRedirectIntFromByteArrayBoundary() {
        byte[] testBytes = {(byte) 0x12, (byte) 0x34};
        int result = redirectIntFromByteArrayOptimized(testBytes, 0, 2);
        assertEquals(0, result);
    }

    @Test
    public void testSendCmdResponseLogic() throws IOException {
        String message = "Hello World!";
        byte[] fullBytes = message.getBytes(StandardCharsets.UTF_8);
        int len = fullBytes.length;
        int offset = 0;
        byte[] chunkBuffer = new byte[4096];
        int chunkCount = 0;

        while (offset < len) {
            int chunkSize = Math.min(4096, len - offset);
            System.arraycopy(fullBytes, offset, chunkBuffer, 0, chunkSize);
            offset += chunkSize;
            chunkCount++;
            byte[] expectedChunk = new byte[chunkSize];
            System.arraycopy(fullBytes, offset - chunkSize, expectedChunk, 0, chunkSize);
            for (int i = 0; i < chunkSize; i++) {
                assertEquals(expectedChunk[i], chunkBuffer[i]);
            }
        }
        assertEquals(1, chunkCount);
    }
}