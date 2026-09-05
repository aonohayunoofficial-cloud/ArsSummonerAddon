package com.example.an_addon.summon;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SummonBase implements StringRepresentable {
    // id, 表示名, 騎乗可, 憑依可, 騎乗速度係数
    // 係数は移動速度属性 0.30 に掛かる。実効値の目安:
    //   獣 0.150 / スライム 0.120 / 岩 0.102
    //   （プレイヤー歩行 0.10、ダッシュ 0.13、馬 0.11〜0.34）
    SLIME("slime", "スライム", true, false, 0.40F),
    BEAST("beast", "獣", true, false, 0.50F),
    ROCK("rock", "岩", true, false, 0.34F),
    LAMP("lamp", "灯", false, true, 0.40F),
    BONE("bone", "骨", false, true, 0.40F);

    public static final Codec<SummonBase> CODEC = StringRepresentable.fromEnum(SummonBase::values);

    private final String id;
    private final String displayName;
    private final boolean rideable;
    private final boolean possessable;
    private final float rideSpeedFactor;

    SummonBase(String id, String displayName, boolean rideable, boolean possessable, float rideSpeedFactor) {
        this.id = id;
        this.displayName = displayName;
        this.rideable = rideable;
        this.possessable = possessable;
        this.rideSpeedFactor = rideSpeedFactor;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRideable() {
        return rideable;
    }

    public boolean isPossessable() {
        return possessable;
    }

    /** 移動速度属性に掛ける係数。獣が速く、岩が遅い */
    public float getRideSpeedFactor() {
        return rideSpeedFactor;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static SummonBase byId(String id) {
        for (SummonBase b : values()) {
            if (b.id.equalsIgnoreCase(id)) return b;
        }
        return SLIME;
    }
}
