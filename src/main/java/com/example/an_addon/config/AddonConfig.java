package com.example.an_addon.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AddonConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SHOW_EXP_GAIN;
    public static final ModConfigSpec.BooleanValue SHOW_LEVEL_UP;
    public static final ModConfigSpec.BooleanValue SHOW_ABILITY_UNLOCK;
    public static final ModConfigSpec.BooleanValue SHOW_STATE_MESSAGE;
    public static final ModConfigSpec.DoubleValue RIDE_SPEED_MULTIPLIER;
    public static final ModConfigSpec.IntValue POSSESS_LIGHT_LEVEL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("messages");
        SHOW_EXP_GAIN = b.comment("経験値取得をアクションバーに表示")
                .define("showExpGain", false);
        SHOW_LEVEL_UP = b.comment("レベルアップを表示")
                .define("showLevelUp", true);
        SHOW_ABILITY_UNLOCK = b.comment("能力解放を表示")
                .define("showAbilityUnlock", true);
        SHOW_STATE_MESSAGE = b.comment("召喚・帰還・憑依などの状態変化を表示")
                .define("showStateMessage", true);
        b.pop();

        b.push("ride");
        RIDE_SPEED_MULTIPLIER = b.comment("騎乗速度の全体倍率")
                .defineInRange("speedMultiplier", 1.0D, 0.1D, 3.0D);
        b.pop();

        b.push("possess");
        POSSESS_LIGHT_LEVEL = b.comment("灯の憑依中に発生する光源レベル")
                .defineInRange("lightLevel", 8, 0, 15);
        b.pop();

        SPEC = b.build();
    }
}
