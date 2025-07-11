package me.steinborn.krypton.mod.shared.config.compress;

import one.pkg.config.annotation.loader.ReadAction;

public class CompressionLevel {
    public static int value = 4;

    @ReadAction
    public static void read() {
        if (value > 9 || value < 1) {
            value = 4;
        }
    }
}
