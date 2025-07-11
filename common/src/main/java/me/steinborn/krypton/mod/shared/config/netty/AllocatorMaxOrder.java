package me.steinborn.krypton.mod.shared.config.netty;

import one.pkg.config.annotation.loader.ReadAction;

public class AllocatorMaxOrder {
    public static int value = 9;

    @ReadAction
    public static void run() {
        if (value < 1 || value > 50) {
            value = 9;
        }
    }
}
