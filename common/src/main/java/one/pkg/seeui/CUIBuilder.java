package one.pkg.seeui;

import net.minecraft.client.gui.screens.Screen;

public class CUIBuilder {
    private Class<?> clazz;
    private Screen lastScreen;
    private Runnable onSaved;

    public static CUIBuilder builder() {
        return new CUIBuilder();
    }

    public CUIBuilder clazz(Class<?> clazz) {
        this.clazz = clazz;
        return this;
    }

    public CUIBuilder lastScreen(Screen lastScreen) {
        this.lastScreen = lastScreen;
        return this;
    }

    public CUIBuilder onSaved(Runnable onSaved) {
        this.onSaved = onSaved;
        return this;
    }

    public Screen build() {
        return new ConfigScreen(clazz, lastScreen, onSaved);
    }
}
