package one.pkg.kreno.shared.network.netty;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import one.pkg.kreno.shared.network.util.SystemInfo;

public class NettyUtil {
    public static Class<? extends DatagramChannel> getChannelClass(boolean useNativeTransport) {
        if (!useNativeTransport) {
            return NioDatagramChannel.class;
        }

        if (SystemInfo.IS_MAC) {
            if (KQueue.isAvailable()) {
                return KQueueDatagramChannel.class;
            }
        } else if (SystemInfo.IS_LINUX) {
            if (Epoll.isAvailable()) {
                return EpollDatagramChannel.class;
            }
        }

        return NioDatagramChannel.class;
    }
}
