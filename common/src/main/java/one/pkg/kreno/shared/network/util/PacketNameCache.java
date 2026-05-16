package one.pkg.kreno.shared.network.util;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import one.pkg.libsl.utils.map.WeakConcurrentHashMap;

import java.util.Map;

public class PacketNameCache {
    private static final Map<Class<?>, String> PACKET_NAME_CACHE = new WeakConcurrentHashMap<>();
    private static final Map<Object, String> CUSTOM_PAYLOAD_CACHE = new WeakConcurrentHashMap<>();

    public static String getPacketName(Object packet) {
        if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)) {
            var id = CUSTOM_PAYLOAD_CACHE.get(payload.type().id());
            if (id != null) {
                return id;
            }
            id = "ClientboundCustomPayloadPacket[" + payload.type().id() + "]";
            CUSTOM_PAYLOAD_CACHE.put(payload.type().id(), id);
            return id;
        } else if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
            var id = CUSTOM_PAYLOAD_CACHE.get(payload.type().id());
            if (id != null) {
                return id;
            }
            id = "ServerboundCustomPayloadPacket[" + payload.type().id() + "]";
            CUSTOM_PAYLOAD_CACHE.put(payload.type().id(), id);
            return id;
        }
        return PACKET_NAME_CACHE.computeIfAbsent(packet.getClass(), Class::getSimpleName);
    }
}
