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
        int capBonus,
        int dataVersion
) {
    public static final int CURRENT_DATA_VERSION = 3;

    /** 素のレベル上限。限界突破で capBonus が加算される */
    public static final int LEVEL_CAP = 20;

    /** 限界突破の累計上限（+20 で実質 Lv40 まで） */
    public static final int MAX_CAP_BONUS = 20;

    /** 限界突破1回あたりの上限上昇量 */
    public static final int BREAKTHROUGH_STEP = 5;

    /** ティアの上限 */
    public static final int MAX_TIER = 5;

    /** 騎乗・憑依が解放されるレベル */
    public static final int RELEASE_LEVEL = 5;

    public static final SummonData DEFAULT = new SummonData(
            SummonBase.SLIME, SummonElement.NONE, 1, 1, 0, 50, false, Optional.empty(), 0, CURRENT_DATA_VERSION);

    public static final Codec<SummonData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            SummonBase.CODEC.optionalFieldOf("base", SummonBase.SLIME).forGetter(SummonData::base),
            SummonElement.CODEC.optionalFieldOf("element", SummonElement.NONE).forGetter(SummonData::element),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(SummonData::tier),
            Codec.INT.optionalFieldOf("level", 1).forGetter(SummonData::level),
            Codec.INT.optionalFieldOf("exp", 0).forGetter(SummonData::exp),
            Codec.INT.optionalFieldOf("relationship", 50).forGetter(SummonData::relationship),
            Codec.BOOL.optionalFieldOf("possessing", false).forGetter(SummonData::possessing),
            UUIDUtil.CODEC.optionalFieldOf("entityId").forGetter(SummonData::entityId),
            Codec.INT.optionalFieldOf("capBonus", 0).forGetter(SummonData::capBonus),
            Codec.INT.optionalFieldOf("dataVersion", CURRENT_DATA_VERSION).forGetter(SummonData::dataVersion)
    ).apply(inst, SummonData::new));

    public static final StreamCodec<ByteBuf, SummonData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** この札の実効レベル上限 */
    public int levelCap() {
        return LEVEL_CAP + Math.max(0, Math.min(MAX_CAP_BONUS, capBonus));
    }

    /** 次のレベルに必要な経験値。上限到達時は 0 */
    public static int expForLevel(int lv) {
        return 10 + lv * 5;
    }

    public int expToNext() {
        return level >= levelCap() ? 0 : expForLevel(level);
    }

    public SummonData withEntity(Optional<UUID> id) {
        return new SummonData(base, element, tier, level, exp, relationship, possessing, id, capBonus, dataVersion);
    }

    public SummonData withLevelExp(int newLevel, int newExp) {
        return new SummonData(base, element, tier, newLevel, newExp, relationship, possessing, entityId, capBonus, dataVersion);
    }

    public SummonData withRelationship(int value) {
        return new SummonData(base, element, tier, level, exp,
                Math.max(0, Math.min(100, value)), possessing, entityId, capBonus, dataVersion);
    }

    public SummonData withBaseElement(SummonBase newBase, SummonElement newElement) {
        return new SummonData(newBase, newElement, tier, level, exp, relationship, possessing, entityId, capBonus, dataVersion);
    }

    public SummonData withPossessing(boolean value) {
        return new SummonData(base, element, tier, level, exp, relationship, value, entityId, capBonus, dataVersion);
    }

    public SummonData withTier(int newTier) {
        return new SummonData(base, element,
                Math.max(1, Math.min(MAX_TIER, newTier)),
                level, exp, relationship, possessing, entityId, capBonus, dataVersion);
    }

    public SummonData grantExp(int amount) {
        int cap = levelCap();
        if (level >= cap) return this;
        int lv = level;
        int xp = exp + Math.max(0, amount);
        while (lv < cap) {
            int need = expForLevel(lv);
            if (xp < need) break;
            xp -= need;
            lv++;
        }
        if (lv >= cap) xp = 0;
        return withLevelExp(lv, xp);
    }

    public SummonData withLevelForced(int newLevel) {
        return withLevelExp(Math.max(1, Math.min(levelCap(), newLevel)), 0);
    }

    public boolean canRide() {
        return level >= RELEASE_LEVEL && base.isRideable();
    }

    public boolean canPossess() {
        return level >= RELEASE_LEVEL && base.isPossessable();
    }

    /** レベル上限に到達しているか */
    public boolean isAtCap() {
        return level >= levelCap();
    }

    public boolean canEvolve() {
        return isAtCap() && tier < MAX_TIER;
    }

    public boolean canBreakthrough() {
        return isAtCap() && capBonus < MAX_CAP_BONUS;
    }

    /** 進化: ティア +1、レベルは 1 に戻る。関係値と限界突破分は維持 */
    public SummonData evolve() {
        return new SummonData(base, element,
                Math.min(MAX_TIER, tier + 1),
                1, 0, relationship, false, Optional.empty(), capBonus, CURRENT_DATA_VERSION);
    }

    /** 限界突破: レベル上限 +5。レベルはそのまま */
    public SummonData breakthrough() {
        return new SummonData(base, element, tier, level, 0, relationship, possessing, entityId,
                Math.min(MAX_CAP_BONUS, capBonus + BREAKTHROUGH_STEP), CURRENT_DATA_VERSION);
    }
}
