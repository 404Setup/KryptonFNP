package one.pkg.kreno.shared.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.libsl.api.ui.seeui.SeeUIBuilder;

public class KRenoConfigGUI extends Screen {
    private final Screen parent;

    public KRenoConfigGUI(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        minecraft.gui.setScreen(SeeUIBuilder.builder()
                .clazz(ModConfig.class)
                .lastScreen(parent)
                .useOreUI(ModConfig.GUI.isOreUI())
                .onSaved(() -> {
                    try {
                        ModConfig.config.saveAllConfigurations();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .build());
    }
}