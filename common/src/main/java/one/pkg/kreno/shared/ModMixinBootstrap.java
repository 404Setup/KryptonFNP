package one.pkg.kreno.shared;

import one.pkg.loader.Loader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ModMixinBootstrap implements IMixinConfigPlugin {
    private final Logger logger = LoggerFactory.getLogger("KryptonReno ModMixin");

    public ModMixinBootstrap() {
        ModConfig.config.addConfigurations(); // Initialize it
    }

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        CONFIG config = CONFIG.find(mixinClassName);
        boolean enabled = config == null || config.isEnabled();

        if (enabled) {
            Compatibility compatibility = Compatibility.find(mixinClassName);
            if (compatibility != null && Loader.INSTANCE.loaded(compatibility.modId)) {
                if (compatibility.type == CompatibilityType.DISABLE || compatibility.type == CompatibilityType.ChangeMixinTarget) {
                    enabled = false;
                }
            } else {
                Compatibility alternative = Compatibility.findAlternative(mixinClassName);
                if (alternative != null) {
                    enabled = Loader.INSTANCE.loaded(alternative.modId);
                }
            }
        }

        logger.info("Mixin {} {}", mixinClassName, enabled ? "enabled" : "disabled");
        return enabled;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    enum Compatibility {
        ServerLoginEncryptionWithE4MC(
                "one.pkg.kreno.mixin.network.pipeline.encryption.ServerLoginPacketListenerImplMixin",
                "e4mc",
                CompatibilityType.DISABLE),
        ConnectionEncryptionWithE4MC(
                "one.pkg.kreno.mixin.network.pipeline.encryption.ConnectionMixin",
                "e4mc",
                CompatibilityType.DISABLE),
        ;

        public final String mixinClass;
        public final String modId;
        public final CompatibilityType type;
        public final String alternativeMixin;

        Compatibility(String mixinClass, String modId, CompatibilityType type) {
            this(mixinClass, modId, type, null);
        }

        Compatibility(String mixinClass, String modId, CompatibilityType type, String alternativeMixin) {
            this.mixinClass = mixinClass;
            this.modId = modId;
            this.type = type;
            this.alternativeMixin = alternativeMixin;
        }

        @Nullable
        public static Compatibility find(String mixinClassName) {
            for (Compatibility value : values()) {
                if (value.mixinClass.equals(mixinClassName)) {
                    return value;
                }
            }
            return null;
        }

        @Nullable
        public static Compatibility findAlternative(String mixinClassName) {
            for (Compatibility value : values()) {
                if (value.type == CompatibilityType.ChangeMixinTarget && mixinClassName.equals(value.alternativeMixin)) {
                    return value;
                }
            }
            return null;
        }
    }

    enum CompatibilityType {
        DISABLE, ChangeMixinTarget;
    }

    enum CONFIG {
        ClientEncrypt("one.pkg.kreno.mixin.network.pipeline.encryption.ClientLoginMixin", ModConfig.Mixin::isClientEncrypt),
        RconClient("one.pkg.kreno.mixin.network.experimental.RconClientMixin", ModConfig.Mixin::isRconClient),
        BestVarLong("one.pkg.kreno.mixin.network.microopt.FriendlyByteBufVarLongMixin", ModConfig.Mixin::isBestVarLong),
        BestVarInt("one.pkg.kreno.mixin.network.microopt.FriendlyByteBufVarIntMixin", ModConfig.Mixin::isBestVarInt),
        BestUtf("one.pkg.kreno.mixin.network.microopt.FriendlyByteBufUtfMixin", ModConfig.Mixin::isBestUtf),
        ;

        public final String CLASS;
        public final Supplier<Boolean> configTarget;

        CONFIG(String clazz, @NotNull Supplier<Boolean> configTarget) {
            this.CLASS = clazz;
            this.configTarget = configTarget;
        }

        @Nullable
        public static CONFIG find(String clazz) {
            for (CONFIG config : values()) {
                if (config.CLASS.equals(clazz)) {
                    return config;
                }
            }
            return null;
        }

        public boolean isEnabled() {
            return this.configTarget.get();
        }
    }
}