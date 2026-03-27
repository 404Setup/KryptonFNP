package one.pkg.seeui;

import net.minecraft.client.gui.screens.Screen;

public class SeeUIBuilder {
    private Class<?> clazz;
    private Screen lastScreen;
    private Runnable onSaved;

    public static SeeUIBuilder builder() {
        return new SeeUIBuilder();
    }

    public SeeUIBuilder clazz(Class<?> clazz) {
        this.clazz = clazz;
        return this;
    }

    public SeeUIBuilder lastScreen(Screen lastScreen) {
        this.lastScreen = lastScreen;
        return this;
    }

    public SeeUIBuilder onSaved(Runnable onSaved) {
        this.onSaved = onSaved;
        return this;
    }

    public Screen build() {
        return new SeeUIConfigScreen(clazz, lastScreen, onSaved);
    }
}
