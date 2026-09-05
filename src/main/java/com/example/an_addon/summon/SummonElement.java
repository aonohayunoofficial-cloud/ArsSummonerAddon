package com.example.an_addon.summon;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SummonElement implements StringRepresentable {
    NONE("none", "無", 0xAAAAAA),
    FIRE("fire", "炎", 0xFF5533),
    WATER("water", "水", 0x3388FF),
    EARTH("earth", "地", 0x88663B),
    WIND("wind", "風", 0x88FFBB),
    // 拡張枠（初期リリースでは抽選対象にしない）
    LIGHT("light", "光", 0xFFFFCC),
    DARK("dark", "闇", 0x442255);

    public static final Codec<SummonElement> CODEC = StringRepresentable.fromEnum(SummonElement::values);

    private final String id;
    private final String displayName;
    private final int color;

    SummonElement(String id, String displayName, int color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
