package com.example.an_addon.summon.ability;

public enum AbilityId {
    // Lv5 解放（既存実装）
    RIDE("騎乗"),
    POSSESS("憑依"),
    // Lv10 地形制御
    WALL_CLING("壁張り付き"),
    HIGH_JUMP("高跳び"),
    MAGMA_RESIST("マグマ耐性"),
    HOVER("浮遊"),
    UNDEAD_NEUTRAL("アンデッド非敵対"),
    // Lv15 環境制御
    WATER_WALK("水上歩行"),
    SCENT_DETECT("嗅覚探知"),
    LAVA_SWIM("溶岩泳ぎ"),
    DARKNESS_IMMUNE("暗闇無効"),
    STEALTH("隠密");

    private final String displayName;

    AbilityId(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
