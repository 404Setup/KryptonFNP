package me.steinborn.krypton.mod.shared;

import net.xstopho.resourceconfigapi.annotations.Config;
import net.xstopho.resourceconfigapi.annotations.ConfigEntry;
import net.xstopho.resourceconfigapi.api.ConfigType;

@Config(fileName = "krypton_fnp", type = ConfigType.COMMON)
public class KryptonFNPModConfig {
    @ConfigEntry(category = "General", needsGameRestart = true)
    public static boolean bestVarLong = true;

    @ConfigEntry(category = "General", needsGameRestart = true)
    public static boolean utilVT = true;

    @ConfigEntry(category = "General", needsGameRestart = true)
    public static boolean loginVT = true;

    @ConfigEntry(category = "General", needsGameRestart = true)
    public static boolean textFilterVT = true;

    @ConfigEntry(category = "Netty", needsGameRestart = true)
    public static int allocatorMaxOrder = 9;

    @ConfigEntry(category = "BugFix")
    public static boolean kryptonIssues128 = false;

    @ConfigEntry(category = "BugFix")
    public static boolean kryptonIssues128Sync = true;

    @ConfigEntry(category = "Compress", needsGameRestart = true)
    public static boolean permitOversizedPackets = false;
}
