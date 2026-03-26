package one.pkg.seeui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class ConfigAnnotations {

    public enum Mode {
        TEXT,
        CYCLE,
        SLIDER
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Entry {
        String category() default "general";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Range {
        double min() default Double.NEGATIVE_INFINITY;

        double max() default Double.POSITIVE_INFINITY;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface DisplayMode {
        Mode value() default Mode.TEXT;

        String[] cycleValues() default {};
    }
}
