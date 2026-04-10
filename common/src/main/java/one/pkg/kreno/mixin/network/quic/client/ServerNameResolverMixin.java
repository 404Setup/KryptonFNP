package one.pkg.kreno.mixin.network.quic.client;

import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.kreno.shared.network.quic.DNSLookup;
import one.pkg.kreno.shared.network.quic.QuicSocketAddress;
import one.pkg.kreno.shared.network.quic.ServerAddressProperties;
import one.pkg.libsl.loader.JavaLoader;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;
import java.util.Optional;

@Mixin(ServerNameResolver.class)
public class ServerNameResolverMixin {

    @Shadow(remap = false)
    @Final
    private ServerRedirectHandler redirectHandler;

    @Redirect(
            method = "resolveAddress(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Ljava/util/Optional;",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/multiplayer/resolver/ServerNameResolver;redirectHandler:Lnet/minecraft/client/multiplayer/resolver/ServerRedirectHandler;",
                    opcode = Opcodes.GETFIELD),
            remap = false
    )
    private ServerRedirectHandler getRedirectHandler(ServerNameResolver self, ServerAddress address) {
        if (!ModConfig.Quic.isDisableQuic() && JavaLoader.INSTANCE.loaded("kreno_addons_quic")) {
            if (((ServerAddressProperties) (Object) address).getUseQuic()) {
                return DNSLookup.INSTANCE;
            } else {
                return serverAddress -> {
                    var vanillaResult = redirectHandler.lookupRedirect(serverAddress);
                    if (vanillaResult.isPresent()) {
                        return vanillaResult;
                    }
                    return DNSLookup.INSTANCE.lookupRedirect(serverAddress);
                };
            }
        }
        return redirectHandler;
    }

    @Redirect(
            method = "resolveAddress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/resolver/ServerAddressResolver;resolve(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Ljava/util/Optional;"
            ),
            remap = false
    )
    private Optional<ResolvedServerAddress> wrapResolvedAddress(ServerAddressResolver resolver, ServerAddress address) {
        Optional<ResolvedServerAddress> result = resolver.resolve(address);
        if (!ModConfig.Quic.isDisableQuic() && JavaLoader.INSTANCE.loaded("kreno_addons_quic")) {
            return result.map(rsa -> {
                InetSocketAddress isa = rsa.asInetSocketAddress();
                return ResolvedServerAddress.from(new QuicSocketAddress(isa.getAddress(), isa.getPort(), address));
            });
        }
        return result;
    }
}
