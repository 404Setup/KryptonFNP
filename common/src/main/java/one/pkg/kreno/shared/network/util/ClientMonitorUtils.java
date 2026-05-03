package one.pkg.kreno.shared.network.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.world.entity.player.Player;

public class ClientMonitorUtils {
    public static Player onMonitor(PacketListener listener) {
        try {
            if (listener instanceof ClientPacketListener) {
                return Minecraft.getInstance().player;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
