package one.pkg.seeui.annotations;

import one.pkg.seeui.EntryMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DisplayMode {
    EntryMode value() default EntryMode.TEXT;

    String[] cycleValues() default {};
}