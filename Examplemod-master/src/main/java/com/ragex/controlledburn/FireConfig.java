package com.ragex.controlledburn;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class FireConfig
{
    public static Configuration config;

    public static GlobalMultipliers globalMultipliers = new GlobalMultipliers();
    public static SpecialToggles specialToggles = new SpecialToggles();
    public static BurnSpreadChances burnSpreadChances = new BurnSpreadChances();
    public static FireSpreadRanges spreadRanges = new FireSpreadRanges();
    public static SpreadStrengths spreadStrengths = new SpreadStrengths();

    public static String[] blockSettings = {};
    public static String[] blockTransformations = {};
    public static String[] fireSourceBlocks = {};
    public static String[] blockSpreadsFire = {};

    /* ------------------------------------------------------------------------- */
    /* Init / load                                                               */
    /* ------------------------------------------------------------------------- */

    public static void init(File file)
    {
        config = new Configuration(file);
        sync();
    }

    public static void sync()
    {
        /* ---------------- global multipliers ---------------- */

        globalMultipliers.burnSpeedMultiplier =
                config.get("globalMultipliers", "burnSpeedMultiplier", 1.0,
                                "Flammability multiplier (0 disables block burning)")
                        .getDouble(1.0);

        globalMultipliers.spreadSpeedMultiplier =
                config.get("globalMultipliers", "spreadSpeedMultiplier", 1.0,
                                "Encouragement multiplier")
                        .getDouble(1.0);

        globalMultipliers.tickDelay =
                config.get("globalMultipliers", "tickDelay", 30,
                                "Ticks between fire updates (min 1)")
                        .getInt(30);

        globalMultipliers.tickDelayRandomization =
                config.get("globalMultipliers", "tickDelayRandomization", 9,
                                "Random ticks added to delay")
                        .getInt(9);

        /* ---------------- special toggles ---------------- */

        specialToggles.ignoreHumidBiomes =
                config.get("specialToggles", "ignoreHumidBiomes", false).getBoolean(false);

        specialToggles.ignoreRain =
                config.get("specialToggles", "ignoreRain", false).getBoolean(false);

        specialToggles.noLavaFire =
                config.get("specialToggles", "noLavaFire", false).getBoolean(false);

        specialToggles.noLightningFire =
                config.get("specialToggles", "noLightningFire", false).getBoolean(false);

        specialToggles.losFireSpread =
                config.get("specialToggles", "losFireSpread", false).getBoolean(false);

        /* ---------------- burn spread chances ---------------- */

        burnSpreadChances.maxBurnSpreadChance =
                config.get("burnSpreadChances", "maxBurnSpreadChance", 80)
                        .getInt(80);

        burnSpreadChances.minBurnSpreadChance =
                config.get("burnSpreadChances", "minBurnSpreadChance", 50)
                        .getInt(50);

        /* ---------------- fire spread ranges ---------------- */

        spreadRanges.reachAbove =
                config.get("spreadRanges", "reachAbove", 4).getInt(4);

        spreadRanges.reachBelow =
                config.get("spreadRanges", "reachBelow", 1).getInt(1);

        spreadRanges.reachHorizontal =
                config.get("spreadRanges", "reachHorizontal", 1).getInt(1);

        /* ---------------- spread strengths ---------------- */

        spreadStrengths.burnSpreadStrength =
                config.get("spreadStrengths", "burnSpreadStrength", -1)
                        .getInt(-1);

        spreadStrengths.naturalSpreadStrength =
                config.get("spreadStrengths", "naturalSpreadStrength", -1)
                        .getInt(-1);

        /* ---------------- block arrays ---------------- */

        blockSettings =
                config.get("blockSpecific", "blockSettings", new String[]{}).getStringList();

        blockTransformations =
                config.get("blockTransformations", "blockTransformations", new String[]{}).getStringList();

        fireSourceBlocks =
                config.get("fireSourceBlocks", "fireSourceBlocks", new String[]{}).getStringList();

        blockSpreadsFire =
                config.get("blockSpreadsFire", "blockSpreadsFire", new String[]{}).getStringList();

        if (config.hasChanged()) config.save();
    }

    /* ------------------------------------------------------------------------- */
    /* Data containers                                                           */
    /* ------------------------------------------------------------------------- */

    public static class GlobalMultipliers
    {
        public double burnSpeedMultiplier = 1;
        public double spreadSpeedMultiplier = 1;
        public int tickDelay = 30;
        public int tickDelayRandomization = 9;
    }

    public static class SpecialToggles
    {
        public boolean ignoreHumidBiomes = false;
        public boolean ignoreRain = false;
        public boolean noLavaFire = false;
        public boolean noLightningFire = false;
        public boolean losFireSpread = false;
    }

    public static class BurnSpreadChances
    {
        public int maxBurnSpreadChance = 80;
        public int minBurnSpreadChance = 50;
    }

    public static class FireSpreadRanges
    {
        public int reachAbove = 4;
        public int reachBelow = 1;
        public int reachHorizontal = 1;
    }

    public static class SpreadStrengths
    {
        public int burnSpreadStrength = -1;
        public int naturalSpreadStrength = -1;
    }
}
