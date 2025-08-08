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
    static {
        ModConfig.config.addConfigurations(); // Initialize it
    }

    private final Logger logger = LoggerFactory.getLogger("ModMixinBootstrap");

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
            logger.info("Mixin {} {} {}", mixinClassName, b ? "enabled" : "disabled", config.ENV);
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
        Login_VT("one.pkg.mod.krypton_fnp.mixin.network.experimental.ServerLoginPacketListenerImplMixin", "krypton.loginVT", ModConfig.Mixin::isLoginVT),
        TextFilter_VT("one.pkg.mod.krypton_fnp.mixin.network.experimental.ServerTextFilterMixin", "krypton.textFilterVT", ModConfig.Mixin::isTextFilterVT),
        Util_VT("one.pkg.mod.krypton_fnp.mixin.network.experimental.UtilMixin", "krypton.utilVT", ModConfig.Mixin::isUtilVT),
        BestVarLong("one.pkg.mod.krypton_fnp.mixin.network.experimental.VarLongMixin", "krypton.bestVarLong", ModConfig.Mixin::isBestVarLong),
        ;

        public final String CLASS;
        public final String ENV;
        public final Supplier<Boolean> configTarget;

        CONFIG(String clazz, String env, @NotNull Supplier<Boolean> configTarget) {
            this.CLASS = clazz;
            this.ENV = env;
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
            return this.configTarget.get() || Boolean.parseBoolean(System.getProperty(ENV, "true"));
        }
    }
}
