package one.pkg.kfnp.shared.gui;

import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import one.pkg.loader.Loader;

public class KFNPConfigGUI extends Screen {
    private final Screen parent;

    public KFNPConfigGUI(Screen parent) {
        super(Component.nullToEmpty(""));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Util.getPlatform().openUri(Loader.INSTANCE.getConfigPath().resolve("krypton_fnp.yaml").toUri());
        minecraft.setScreen(parent);
    }
}