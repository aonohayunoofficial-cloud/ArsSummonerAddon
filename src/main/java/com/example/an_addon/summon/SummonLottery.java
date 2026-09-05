package com.example.an_addon.summon;

import net.minecraft.util.RandomSource;

import java.util.EnumMap;
import java.util.Map;

public final class SummonLottery {
    private SummonLottery() {}

    private static final Map<SummonBase, Integer> BASE_WEIGHTS = new EnumMap<>(SummonBase.class);
    private static final Map<SummonElement, Integer> ELEMENT_WEIGHTS = new EnumMap<>(SummonElement.class);

    static {
        BASE_WEIGHTS.put(SummonBase.SLIME, 30);
        BASE_WEIGHTS.put(SummonBase.BEAST, 25);
        BASE_WEIGHTS.put(SummonBase.ROCK,  20);
        BASE_WEIGHTS.put(SummonBase.LAMP,  15);
        BASE_WEIGHTS.put(SummonBase.BONE,  10);

        ELEMENT_WEIGHTS.put(SummonElement.NONE,  20);
        ELEMENT_WEIGHTS.put(SummonElement.FIRE,  15);
        ELEMENT_WEIGHTS.put(SummonElement.WATER, 15);
        ELEMENT_WEIGHTS.put(SummonElement.EARTH, 15);
        ELEMENT_WEIGHTS.put(SummonElement.WIND,  15);
        ELEMENT_WEIGHTS.put(SummonElement.LIGHT, 10);
        ELEMENT_WEIGHTS.put(SummonElement.DARK,  10);
    }

    /**
     * 新規契約はティア固定で T1。ティアは進化でのみ上がる。
     * 抽選で決まるのはベースと属性のみ。
     */
    public static SummonData roll(RandomSource random) {
        SummonBase base = pick(BASE_WEIGHTS, random, SummonBase.SLIME);
        SummonElement element = pick(ELEMENT_WEIGHTS, random, SummonElement.NONE);
        return SummonData.DEFAULT
                .withBaseElement(base, element)
                .withTier(1);
    }

    private static <T> T pick(Map<T, Integer> weights, RandomSource random, T fallback) {
        int total = 0;
        for (int w : weights.values()) total += w;
        if (total <= 0) return fallback;
        int r = random.nextInt(total);
        for (Map.Entry<T, Integer> e : weights.entrySet()) {
            r -= e.getValue();
            if (r < 0) return e.getKey();
        }
        return fallback;
    }
}
