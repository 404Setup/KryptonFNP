package me.steinborn.krypton.mod.shared;

import net.xstopho.resourceconfigapi.annotations.Config;
import net.xstopho.resourceconfigapi.annotations.ConfigEntry;
import net.xstopho.resourceconfigapi.api.ConfigType;

@Config(fileName = "krypton_fnp", type = ConfigType.COMMON)
public class KryptonFNPModConfig {
    @ConfigEntry(category = "General")
    public static boolean bestVarLong = true;

    @ConfigEntry(category = "General")
    public static boolean utilVT = true;

    @ConfigEntry(category = "General")
    public static boolean loginVT = true;

    @ConfigEntry(category = "General")
    public static boolean textFilterVT = true;

    @ConfigEntry(category = "BugFix")
    public static boolean kryptonIssues128 = false;

    @ConfigEntry(category = "BugFix")
    public static boolean kryptonIssues128Sync = true;
}
