package one.pkg.kreno.shared.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import one.pkg.kreno.shared.ModConfig;
import one.pkg.seeui.SeeUIBuilder;

public class KRenoSimpleConfigGUI extends Screen {
    private final Screen parent;

    public KRenoSimpleConfigGUI(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        minecraft.setScreen(SeeUIBuilder.builder()
                .clazz(ModConfig.class)
                .lastScreen(parent)
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