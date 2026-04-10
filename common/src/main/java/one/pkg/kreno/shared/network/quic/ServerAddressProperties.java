package one.pkg.kreno.shared.network.quic;

public interface ServerAddressProperties {

    boolean getUseQuic();

    void setUseQuic(boolean quic);

    default void copy(ServerAddressProperties other) {
        setUseQuic(other.getUseQuic());
    }
}
