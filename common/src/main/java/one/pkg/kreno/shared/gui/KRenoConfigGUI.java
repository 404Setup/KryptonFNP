package one.pkg.kreno.shared.gui;

import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import one.pkg.loader.Loader;

public class KRenoConfigGUI extends Screen {
    private final Screen parent;

    public KRenoConfigGUI(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        Util.getPlatform().openUri(Loader.INSTANCE.getConfigPath().resolve("kreno.yaml").toUri());
        minecraft.setScreen(parent);
    }
}
