package com.example.an_addon.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AddonConfig {

    public static final ModConfigSpec SPEC;

    /** 経験値取得時のアクションバー表示 */
    public static final ModConfigSpec.BooleanValue SHOW_EXP_GAIN;
    /** レベルアップ時のチャット表示 */
    public static final ModConfigSpec.BooleanValue SHOW_LEVEL_UP;
    /** 能力解放時のチャット表示 */
    public static final ModConfigSpec.BooleanValue SHOW_ABILITY_UNLOCK;
    /** 送還・召喚などの状態表示 */
    public static final ModConfigSpec.BooleanValue SHOW_STATE_MESSAGE;

    /** 騎乗速度の全体倍率 */
    public static final ModConfigSpec.DoubleValue RIDE_SPEED_MULTIPLIER;
    /** 憑依中の光源の明るさ */
    public static final ModConfigSpec.IntValue POSSESS_LIGHT_LEVEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("画面表示の設定").push("messages");
        SHOW_EXP_GAIN = builder
                .comment("経験値が入ったときにアクションバーへ表示する")
                .define("showExpGain", false);
        SHOW_LEVEL_UP = builder
                .comment("レベルアップをチャットへ表示する")
                .define("showLevelUp", true);
        SHOW_ABILITY_UNLOCK = builder
                .comment("能力解放をチャットへ表示する")
                .define("showAbilityUnlock", true);
        SHOW_STATE_MESSAGE = builder
                .comment("召喚・送還・憑依の状態をアクションバーへ表示する")
                .define("showStateMessage", true);
        builder.pop();

        builder.comment("騎乗の設定").push("ride");
        RIDE_SPEED_MULTIPLIER = builder
                .comment("騎乗速度の全体倍率。1.0 が標準、下げると遅くなる")
                .defineInRange("speedMultiplier", 1.0D, 0.1D, 3.0D);
        builder.pop();

        builder.comment("憑依の設定").push("possess");
        POSSESS_LIGHT_LEVEL = builder
                .comment("灯の憑依で発生する光源の明るさ (0-15)")
                .defineInRange("lightLevel", 8, 0, 15);
        builder.pop();

        SPEC = builder.build();
    }

    private AddonConfig() { }
}
