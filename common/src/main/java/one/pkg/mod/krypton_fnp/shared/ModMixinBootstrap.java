package one.pkg.mod.krypton_fnp.shared;

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
    private final Logger logger = LoggerFactory.getLogger("ModMixinBootstrap");

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
        if (config != null) {
            var b = config.isEnabled();
            logger.info("Mixin {} {}", mixinClassName, b ? "enabled" : "disabled");
            return b;
        }
        return true;
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

    enum CONFIG {
        Login_VT("one.pkg.mod.krypton_fnp.mixin.network.experimental.ServerLoginPacketListenerImplMixin", ModConfig.Mixin::isLoginVT),
        BestVarLong("one.pkg.mod.krypton_fnp.mixin.network.experimental.VarLongMixin", ModConfig.Mixin::isBestVarLong),
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