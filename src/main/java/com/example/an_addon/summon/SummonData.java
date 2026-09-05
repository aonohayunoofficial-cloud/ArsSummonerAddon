package com.example.an_addon.summon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

public record SummonData(
        SummonBase base,
        SummonElement element,
        int tier,
        int level,
        int exp,
        int relationship,
        boolean possessing,
        Optional<UUID> entityId,
        int dataVersion
) {
    public static final int CURRENT_DATA_VERSION = 2;
    public static final int LEVEL_CAP = 20;

    /** 騎乗・憑依が解放されるレベル */
    public static final int RELEASE_LEVEL = 5;

    public static final SummonData DEFAULT = new SummonData(
            SummonBase.SLIME, SummonElement.NONE, 1, 1, 0, 50, false, Optional.empty(), CURRENT_DATA_VERSION);

    public static final Codec<SummonData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            SummonBase.CODEC.optionalFieldOf("base", SummonBase.SLIME).forGetter(SummonData::base),
            SummonElement.CODEC.optionalFieldOf("element", SummonElement.NONE).forGetter(SummonData::element),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(SummonData::tier),
            Codec.INT.optionalFieldOf("level", 1).forGetter(SummonData::level),
            Codec.INT.optionalFieldOf("exp", 0).forGetter(SummonData::exp),
            Codec.INT.optionalFieldOf("relationship", 50).forGetter(SummonData::relationship),
            Codec.BOOL.optionalFieldOf("possessing", false).forGetter(SummonData::possessing),
            UUIDUtil.CODEC.optionalFieldOf("entity").forGetter(SummonData::entityId),
            Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(SummonData::dataVersion)
    ).apply(inst, SummonData::new));

    public static final StreamCodec<ByteBuf, SummonData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public SummonData withEntity(Optional<UUID> id) {
        return new SummonData(base, element, tier, level, exp, relationship, possessing, id, dataVersion);
    }

    public SummonData withLevelExp(int newLevel, int newExp) {
        return new SummonData(base, element, tier, newLevel, newExp, relationship, possessing, entityId, dataVersion);
    }

    public SummonData withRelationship(int value) {
        return new SummonData(base, element, tier, level, exp,
                Math.max(0, Math.min(100, value)), possessing, entityId, dataVersion);
    }

    public SummonData withPossessing(boolean value) {
        return new SummonData(base, element, tier, level, exp, relationship, value, entityId, dataVersion);
    }

    public SummonData withBaseElement(SummonBase newBase, SummonElement newElement) {
        return new SummonData(newBase, newElement, tier, level, exp, relationship, possessing, entityId, dataVersion);
    }

    public static int expForLevel(int lv) {
        return 10 + lv * 5;
    }

    public int expToNext() {
        return expForLevel(level);
    }

    public SummonData grantExp(int amount) {
        int lv = level;
        int xp = exp + amount;
        while (lv < LEVEL_CAP) {
            int need = expForLevel(lv);
            if (xp < need) break;
            xp -= need;
            lv++;
        }
        if (lv >= LEVEL_CAP) xp = 0;
        return withLevelExp(lv, xp);
    }

    public SummonData withLevelForced(int newLevel) {
        return withLevelExp(Math.max(1, Math.min(LEVEL_CAP, newLevel)), 0);
    }

    public SummonData withTier(int newTier) {
        return new SummonData(base, element,
                Math.max(1, Math.min(5, newTier)),
                level, exp, relationship, possessing, entityId, dataVersion);
    }

    public boolean canRide() {
        return level >= RELEASE_LEVEL && base.isRideable();
    }

    public boolean canPossess() {
        return level >= RELEASE_LEVEL && base.isPossessable();
    }
}
