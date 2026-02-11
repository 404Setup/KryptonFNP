## 2025-02-19 - FriendlyByteBuf Allocation Overhead
**Learning:** `FriendlyByteBuf` is often used as a convenient wrapper around Netty's `ByteBuf` for `read/writeVarInt`. However, instantiating it per-packet in hot paths (like compression/decompression) adds unnecessary allocation overhead. Static utility methods operating directly on `ByteBuf` are preferred for performance-critical code.
**Action:** Replace `new FriendlyByteBuf(buf)` with `VarIntUtil` static methods in high-throughput network handlers.
