package one.pkg.seeui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import one.pkg.config.annotation.config.ConfigTarget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigScreen extends OptionsSubScreen {
    private final Class<?> configClass;
    private final Runnable onSaved;

    public ConfigScreen(Class<?> configClass, Screen lastScreen, Runnable onSaved) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable("gui.kreno.config.title"));
        this.configClass = configClass;
        this.onSaved = onSaved;
    }

    @Override
    protected void addOptions() {
        if (this.list == null) return;
        parseConfig();
    }

    @Override
    protected void addFooter() {
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, (_) -> {
            if (onSaved != null) onSaved.run();
            this.minecraft.setScreen(this.lastScreen);
        }).width(200).build());
    }

    private void parseConfig() {
        Map<String, List<ConfigEntry>> categories = new LinkedHashMap<>();
        for (Field field : configClass.getDeclaredFields()) {
            ConfigTarget target = field.getAnnotation(ConfigTarget.class);
            if (target != null) {
                String[] groups = target.group();
                String group = groups.length > 0 ? groups[0] : "general";
                String key = target.value().isEmpty() ? field.getName() : target.value();
                String comment = target.comment();

                ConfigAnnotations.Entry entryAnn = field.getAnnotation(ConfigAnnotations.Entry.class);
                if (entryAnn != null) group = entryAnn.category();

                ConfigAnnotations.Range range = field.getAnnotation(ConfigAnnotations.Range.class);
                double min = range != null ? range.min() : Double.NEGATIVE_INFINITY;
                double max = range != null ? range.max() : Double.POSITIVE_INFINITY;

                ConfigAnnotations.DisplayMode displayMode = field.getAnnotation(ConfigAnnotations.DisplayMode.class);
                ConfigAnnotations.Mode mode = displayMode != null ? displayMode.value() : ConfigAnnotations.Mode.TEXT;

                ConfigEntry entry = createEntry(field, group, key, comment, min, max, mode, displayMode);
                if (entry != null) categories.computeIfAbsent(group, _ -> new ArrayList<>()).add(entry);
            }
        }

        for (Map.Entry<String, List<ConfigEntry>> category : categories.entrySet()) {
            this.list.addHeader(Component.translatable("gui.kreno.config.category." + category.getKey()));
            List<ConfigEntry> entries = category.getValue();
            for (int i = 0; i < entries.size(); i += 2) {
                ConfigEntry entry1 = entries.get(i);
                AbstractWidget widget1 = createWidgetWithAutoSave(entry1);
                if (i + 1 < entries.size()) {
                    ConfigEntry entry2 = entries.get(i + 1);
                    AbstractWidget widget2 = createWidgetWithAutoSave(entry2);
                    this.list.addSmall(widget1, widget2);
                } else {
                    this.list.addSmall(widget1, null);
                }
            }
        }
    }

    private AbstractWidget createWidgetWithAutoSave(ConfigEntry entry) {
        AbstractWidget widget = entry.createWidget(0, 0, 150, 20);
        Component tooltip = entry.getTooltip();
        if (tooltip != null) {
            widget.setTooltip(Tooltip.create(tooltip));
        }
        return widget;
    }

    private ConfigEntry createEntry(Field field, String category, String key, String comment, double min, double max, ConfigAnnotations.Mode mode, ConfigAnnotations.DisplayMode displayMode) {
        Class<?> type = field.getType();
        if (mode == ConfigAnnotations.Mode.CYCLE && displayMode != null)
            return new ConfigEntry.CycleEntry(field, category, key, comment, displayMode.cycleValues());
        if (mode == ConfigAnnotations.Mode.SLIDER)
            return new ConfigEntry.SliderEntry(field, category, key, comment, min, max);

        if (type == boolean.class || type == Boolean.class)
            return new ConfigEntry.BooleanEntry(field, category, key, comment);
        if (type == int.class || type == Integer.class)
            return new ConfigEntry.IntegerEntry(field, category, key, comment, min, max);
        if (type == long.class || type == Long.class)
            return new ConfigEntry.LongEntry(field, category, key, comment, min, max);
        if (type == double.class || type == Double.class || type == float.class || type == Float.class)
            return new ConfigEntry.DoubleEntry(field, category, key, comment, min, max);
        return null;
    }
}
