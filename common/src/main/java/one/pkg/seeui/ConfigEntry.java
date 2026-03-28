package one.pkg.seeui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Field;

public abstract class ConfigEntry {
    protected final Field field;
    protected final String category;
    protected final String key;
    protected final String comment;
    protected final String keyword;

    public ConfigEntry(Field field, String category, String key, String comment, String keyword) {
        this.field = field;
        this.category = category;
        this.key = key;
        this.comment = comment;
        this.keyword = keyword;
        this.field.setAccessible(true);
    }

    public abstract AbstractWidget createWidget(int x, int y, int width, int height);

    public abstract void save();

    public Component getLabel() {
        return Component.translatable(keyword + ".config.entry." + category + "." + key);
    }

    public String getCategory() {
        return category;
    }

    public Component getTooltip() {
        String tooltipKey = keyword + ".config.entry." + category + "." + key + ".desc";
        if (Language.getInstance().has(tooltipKey)) {
            return Component.translatable(tooltipKey);
        }
        if (comment == null || comment.isEmpty()) return null;
        return Component.literal(comment);
    }

    public static class BooleanEntry extends ConfigEntry {
        private boolean value;

        public BooleanEntry(Field field, String category, String key, String comment, String keyword) {
            super(field, category, key, comment, keyword);
            try {
                Object val = field.get(null);
                this.value = val instanceof Boolean ? (Boolean) val : false;
            } catch (IllegalAccessException e) {
                this.value = false;
            }
        }

        @Override
        public AbstractWidget createWidget(int x, int y, int width, int height) {
            return CycleButton.onOffBuilder(value)
                    .create(x, y, width, height, getLabel(), (button, newValue) -> {
                        this.value = newValue;
                        this.save();
                    });
        }

        @Override
        public void save() {
            try {
                field.set(null, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static class IntegerEntry extends ConfigEntry {
        private final double min;
        private final double max;
        private int value;

        public IntegerEntry(Field field, String category, String key, String comment, String keyword, double min, double max) {
            super(field, category, key, comment, keyword);
            this.min = min;
            this.max = max;
            try {
                Object val = field.get(null);
                this.value = val instanceof Number ? ((Number) val).intValue() : 0;
            } catch (IllegalAccessException e) {
                this.value = 0;
            }
        }

        @Override
        public AbstractWidget createWidget(int x, int y, int width, int height) {
            EditBox editBox = new EditBox(Minecraft.getInstance().font, x, y, width, height, getLabel());
            editBox.setValue(String.valueOf(value));
            editBox.setResponder(s -> {
                if (s.isEmpty() || s.equals("-")) return;
                try {
                    int val = Integer.parseInt(s);
                    if (val >= min && val <= max) {
                        this.value = val;
                        this.save();
                    }
                } catch (NumberFormatException ignored) {
                }
            });
            return editBox;
        }

        @Override
        public void save() {
            try {
                if (field.getType() == int.class) field.setInt(null, value);
                else field.set(null, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static class LongEntry extends ConfigEntry {
        private final double min;
        private final double max;
        private long value;

        public LongEntry(Field field, String category, String key, String comment, String keyword, double min, double max) {
            super(field, category, key, comment, keyword);
            this.min = min;
            this.max = max;
            try {
                Object val = field.get(null);
                this.value = val instanceof Number ? ((Number) val).longValue() : 0L;
            } catch (IllegalAccessException e) {
                this.value = 0L;
            }
        }

        @Override
        public AbstractWidget createWidget(int x, int y, int width, int height) {
            EditBox editBox = new EditBox(Minecraft.getInstance().font, x, y, width, height, getLabel());
            editBox.setValue(String.valueOf(value));
            editBox.setResponder(s -> {
                if (s.isEmpty() || s.equals("-")) return;
                try {
                    long val = Long.parseLong(s);
                    if (val >= min && val <= max) {
                        this.value = val;
                        this.save();
                    }
                } catch (NumberFormatException ignored) {
                }
            });
            return editBox;
        }

        @Override
        public void save() {
            try {
                if (field.getType() == long.class) field.setLong(null, value);
                else field.set(null, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static class DoubleEntry extends ConfigEntry {
        private final double min;
        private final double max;
        private double value;

        public DoubleEntry(Field field, String category, String key, String comment, String keyword, double min, double max) {
            super(field, category, key, comment, keyword);
            this.min = min;
            this.max = max;
            try {
                Object val = field.get(null);
                this.value = val instanceof Number ? ((Number) val).doubleValue() : 0.0;
            } catch (IllegalAccessException e) {
                this.value = 0.0;
            }
        }

        @Override
        public AbstractWidget createWidget(int x, int y, int width, int height) {
            EditBox editBox = new EditBox(Minecraft.getInstance().font, x, y, width, height, getLabel());
            editBox.setValue(String.valueOf(value));
            editBox.setResponder(s -> {
                if (s.isEmpty() || s.equals("-") || s.equals(".")) return;
                try {
                    double val = Double.parseDouble(s);
                    if (val >= min && val <= max) {
                        this.value = val;
                        this.save();
                    }
                } catch (NumberFormatException ignored) {
                }
            });
            return editBox;
        }

        @Override
        public void save() {
            try {
                if (field.getType() == double.class) field.setDouble(null, value);
                else if (field.getType() == float.class) field.setFloat(null, (float) value);
                else field.set(null, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static class CycleEntry extends ConfigEntry {
        private final String[] options;
        private String value;

        public CycleEntry(Field field, String category, String key, String comment, String keyword, String[] options) {
            super(field, category, key, comment, keyword);
            this.options = options;
            try {
                Object obj = field.get(null);
                this.value = obj != null ? String.valueOf(obj) : options[0];
            } catch (IllegalAccessException e) {
                this.value = options[0];
            }
        }

        @Override
        public AbstractWidget createWidget(int x, int y, int width, int height) {
            return CycleButton.<String>builder(Component::literal, () -> value)
                    .withValues(options)
                    .create(x, y, width, height, getLabel(), (button, newValue) -> {
                        this.value = newValue;
                        this.save();
                    });
        }

        @Override
        public void save() {
            try {
                Class<?> type = field.getType();
                if (type == String.class) {
                    field.set(null, value);
                } else if (type.isEnum()) {
                    for (Object enumConstant : type.getEnumConstants()) {
                        if (enumConstant.toString().equals(value)) {
                            field.set(null, enumConstant);
                            break;
                        }
                    }
                } else if (type == int.class || type == Integer.class) {
                    int val = Integer.parseInt(value);
                    if (type == int.class) field.setInt(null, val);
                    else field.set(null, val);
                }
            } catch (IllegalAccessException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    public static class SliderEntry extends ConfigEntry {
        private final double min;
        private final double max;
        private final boolean isInteger;
        private double actualValue;

        public SliderEntry(Field field, String category, String key, String comment, String keyword, double min, double max) {
            super(field, category, key, comment, keyword);
            this.min = min;
            this.max = max;
            Class<?> type = field.getType();
            this.isInteger = type == int.class || type == Integer.class || type == long.class || type == Long.class;
            try {
                Number num = (Number) field.get(null);
                this.actualValue = num.doubleValue();
            } catch (IllegalAccessException e) {
                this.actualValue = min;
            }
        }

        @Override
        public AbstractWidget createWidget(int x, int y, int width, int height) {
            return new AbstractSliderButton(x, y, width, height, getLabel(), (actualValue - min) / (max - min)) {
                {
                    updateMessage();
                }

                @Override
                protected void updateMessage() {
                    String valStr;
                    if (isInteger) {
                        valStr = String.valueOf((long) actualValue);
                    } else {
                        valStr = String.format("%.8f", actualValue).replaceAll("\\.?0+$", "");
                    }
                    setMessage(getLabel().copy().append(": ").append(valStr));
                }

                @Override
                protected void applyValue() {
                    double newValue = min + (max - min) * this.value;
                    if (isInteger) {
                        actualValue = Math.round(newValue);
                        this.value = (actualValue - min) / (max - min);
                    } else {
                        double factor = 1e8;
                        actualValue = Math.round(newValue * factor) / factor;
                    }
                    updateMessage();
                    save();
                }

                @Override
                public void updateWidgetNarration(@NonNull NarrationElementOutput output) {
                }
            };
        }

        @Override
        public void save() {
            try {
                if (field.getType() == int.class) field.setInt(null, (int) actualValue);
                else if (field.getType() == long.class) field.setLong(null, (long) actualValue);
                else if (field.getType() == double.class) field.setDouble(null, actualValue);
                else if (field.getType() == float.class) field.setFloat(null, (float) actualValue);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
