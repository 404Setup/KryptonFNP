## 2025-02-19 - FriendlyByteBuf Allocation Overhead
**Learning:** `FriendlyByteBuf` is often used as a convenient wrapper around Netty's `ByteBuf` for `read/writeVarInt`. However, instantiating it per-packet in hot paths (like compression/decompression) adds unnecessary allocation overhead. Static utility methods operating directly on `ByteBuf` are preferred for performance-critical code.
**Action:** Replace `new FriendlyByteBuf(buf)` with `VarIntUtil` static methods in high-throughput network handlers.

## 2025-02-19 - Compression Buffer Under-Allocation
**Learning:** `MinecraftCompressEncoder` (or similar network encoders) allocates a buffer for compressed output based on `uncompressed.readableBytes() + 1`. This is insufficient for the `VarInt` length header (up to 5 bytes) and potential compression overhead/expansion, forcing Netty to reallocate and copy the buffer in many cases (e.g. packets > 128 bytes).
**Action:** Always add a safe margin (e.g., +64 bytes) to initial buffer allocations in compression/encoding handlers to account for headers and overhead, preventing expensive reallocations.
