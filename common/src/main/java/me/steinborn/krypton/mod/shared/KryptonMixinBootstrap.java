package me.steinborn.krypton.mod.shared;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

public class KryptonMixinBootstrap implements IMixinConfigPlugin {
    static {
        KryptonFirstBootstrap.bootstrap();
    }

    private final Logger logger = LoggerFactory.getLogger("KryptonMixinBootstrap");

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
        Login_VT("me.steinborn.krypton.mixin.shared.network.experimental.ServerLoginPacketListenerImplMixin", "krypton.loginVT", getField("loginVT")),
        TextFilter_VT("me.steinborn.krypton.mixin.shared.network.experimental.ServerTextFilterMixin", "krypton.textFilterVT", getField("textFilterVT")),
        Util_VT("me.steinborn.krypton.mixin.shared.network.experimental.UtilMixin", "krypton.utilVT", getField("utilVT")),
        BestVarLong("me.steinborn.krypton.mixin.shared.network.experimental.VarLongMixin", "krypton.bestVarLong", getField("bestVarLong")),
        ;

        public final String CLASS;
        public final String ENV;
        public final Field configTarget;

        CONFIG(String clazz, String env, @NotNull Field configTarget) {
            this.CLASS = clazz;
            this.ENV = env;
            this.configTarget = configTarget;
        }

        private static Field getField(@NotNull String fieldName) {
            try {
                var field = KryptonFNPModConfig.class.getDeclaredField(fieldName);
                if (!Modifier.isStatic(field.getModifiers()))
                    throw new IllegalArgumentException("Field " + fieldName + " is not static.");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        private static @Nullable Object getFieldValue(@NotNull Field field) {
            try {
                return field.get(null);
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        private static boolean getBooleanValue(@NotNull Field field) {
            var value = getFieldValue(field);
            if (value instanceof Boolean b) {
                return b;
            } else if (value instanceof String s) {
                return Boolean.parseBoolean(s);
            } else {
                return false;
            }
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
            return getBooleanValue(this.configTarget) || Boolean.parseBoolean(System.getProperty(ENV, "true"));
        }
    }
}
