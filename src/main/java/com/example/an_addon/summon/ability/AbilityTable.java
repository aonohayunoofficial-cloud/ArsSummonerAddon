package com.example.an_addon.summon.ability;

import com.example.an_addon.summon.SummonBase;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ベース × 解放レベル × 能力ID の対応表。
 * 新しい能力を足すときはここに1行追加するだけ。
 */
public final class AbilityTable {

    public record Entry(int level, AbilityId ability) { }

    private static final Map<SummonBase, List<Entry>> TABLE = new EnumMap<>(SummonBase.class);

    private static void put(SummonBase base, int level, AbilityId ability) {
        TABLE.computeIfAbsent(base, b -> new ArrayList<>()).add(new Entry(level, ability));
    }

    static {
        // スライム: 壁を登り、水面を跳ねる
        put(SummonBase.SLIME, 5, AbilityId.RIDE);
        put(SummonBase.SLIME, 10, AbilityId.WALL_CLING);
        put(SummonBase.SLIME, 15, AbilityId.WATER_WALK);

        // 獣: 跳躍と嗅覚
        put(SummonBase.BEAST, 5, AbilityId.RIDE);
        put(SummonBase.BEAST, 10, AbilityId.HIGH_JUMP);
        put(SummonBase.BEAST, 15, AbilityId.SCENT_DETECT);

        // 岩: 熱に強い
        put(SummonBase.ROCK, 5, AbilityId.RIDE);
        put(SummonBase.ROCK, 10, AbilityId.MAGMA_RESIST);
        put(SummonBase.ROCK, 15, AbilityId.LAVA_SWIM);

        // 灯: 浮遊と視界確保
        put(SummonBase.LAMP, 5, AbilityId.POSSESS);
        put(SummonBase.LAMP, 10, AbilityId.HOVER);
        put(SummonBase.LAMP, 15, AbilityId.DARKNESS_IMMUNE);

        // 骨: 不死者との親和
        put(SummonBase.BONE, 5, AbilityId.POSSESS);
        put(SummonBase.BONE, 10, AbilityId.UNDEAD_NEUTRAL);
        put(SummonBase.BONE, 15, AbilityId.STEALTH);
    }

    public static List<Entry> entriesFor(SummonBase base) {
        return TABLE.getOrDefault(base, List.of());
    }

    /** 現在のレベルで解放済みか */
    public static boolean has(SummonBase base, int level, AbilityId ability) {
        for (Entry e : entriesFor(base)) {
            if (e.ability() == ability && level >= e.level()) return true;
        }
        return false;
    }

    /** 解放済みの能力一覧 */
    public static List<AbilityId> unlocked(SummonBase base, int level) {
        List<AbilityId> list = new ArrayList<>();
        for (Entry e : entriesFor(base)) {
            if (level >= e.level()) list.add(e.ability());
        }
        return list;
    }

    /** 次に解放される能力（無ければ null） */
    public static Entry next(SummonBase base, int level) {
        Entry best = null;
        for (Entry e : entriesFor(base)) {
            if (e.level() > level && (best == null || e.level() < best.level())) {
                best = e;
            }
        }
        return best;
    }

    private AbilityTable() { }
}
