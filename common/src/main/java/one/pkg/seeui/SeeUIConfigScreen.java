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
import one.pkg.seeui.annotations.DisplayMode;
import one.pkg.seeui.annotations.Entry;
import one.pkg.seeui.annotations.Range;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SeeUIConfigScreen extends OptionsSubScreen {
    private final Class<?> configClass;
    private final Runnable onSaved;
    private final String keyword;

    public SeeUIConfigScreen(Class<?> configClass, Screen lastScreen, Runnable onSaved) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable(getKeyword(configClass) + ".config.title"));
        this.configClass = configClass;
        this.onSaved = onSaved;
        this.keyword = getKeyword(configClass);
    }

    private static String getKeyword(Class<?> clazz) {
        one.pkg.config.annotation.config.ConfigEntry ann = clazz.getAnnotation(one.pkg.config.annotation.config.ConfigEntry.class);
        return ann != null ? ann.value() : "gui.kreno";
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

                Entry entryAnn = field.getAnnotation(Entry.class);
                if (entryAnn != null) group = entryAnn.category();

                Range range = field.getAnnotation(Range.class);
                double min = range != null ? range.min() : Double.NEGATIVE_INFINITY;
                double max = range != null ? range.max() : Double.POSITIVE_INFINITY;

                DisplayMode displayMode = field.getAnnotation(DisplayMode.class);
                EntryMode mode = displayMode != null ? displayMode.value() : EntryMode.TEXT;

                ConfigEntry entry = createEntry(field, group, key, comment, min, max, mode, displayMode);
                if (entry != null) categories.computeIfAbsent(group, _ -> new ArrayList<>()).add(entry);
            }
        }

        for (Map.Entry<String, List<ConfigEntry>> category : categories.entrySet()) {
            this.list.addHeader(Component.translatable(keyword + ".config.category." + category.getKey()));
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

    private ConfigEntry createEntry(Field field, String category, String key, String comment, double min, double max, EntryMode mode, DisplayMode displayMode) {
        Class<?> type = field.getType();
        if (mode == EntryMode.CYCLE && displayMode != null)
            return new ConfigEntry.CycleEntry(field, category, key, comment, keyword, displayMode.cycleValues());
        if (mode == EntryMode.SLIDER)
            return new ConfigEntry.SliderEntry(field, category, key, comment, keyword, min, max);

        if (type == boolean.class || type == Boolean.class)
            return new ConfigEntry.BooleanEntry(field, category, key, comment, keyword);
        if (type == int.class || type == Integer.class)
            return new ConfigEntry.IntegerEntry(field, category, key, comment, keyword, min, max);
        if (type == long.class || type == Long.class)
            return new ConfigEntry.LongEntry(field, category, key, comment, keyword, min, max);
        if (type == double.class || type == Double.class || type == float.class || type == Float.class)
            return new ConfigEntry.DoubleEntry(field, category, key, comment, keyword, min, max);
        return null;
    }
}
