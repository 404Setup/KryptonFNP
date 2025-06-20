package me.steinborn.krypton.mod.shared;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class KryptonMixinBootstrap implements IMixinConfigPlugin {
    private final Logger logger = LoggerFactory.getLogger("KryptonMixinBootstrap");

    enum CONFIG {
        Login_VT("me.steinborn.krypton.mixin.shared.network.experimental.ServerLoginPacketListenerImplMixin", "krypton.loginVT"),
        TextFilter_VT("me.steinborn.krypton.mixin.shared.network.experimental.ServerTextFilterMixin", "krypton.textFilterVT"),
        Util_VT("me.steinborn.krypton.mixin.shared.network.experimental.UtilMixin", "krypton.utilVT"),
        BestVarLong("me.steinborn.krypton.mixin.shared.network.experimental.VarLongMixin", "krypton.bestVarLong"),;

        public final String CLASS;
        public final String ENV;

        CONFIG(String clazz, String env) {
            this.CLASS = clazz;
            this.ENV = env;
        }

        public boolean isEnabled() {
            return Boolean.parseBoolean(System.getProperty(ENV, "true"));
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
}
