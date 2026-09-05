package com.example.an_addon.event;

import com.example.an_addon.entity.SummonEntity;
import com.example.an_addon.summon.ability.AbilityId;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

import static com.example.an_addon.ExampleANAddon.MODID;

@EventBusSubscriber(modid = MODID)
public class AbilityEvents {

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player)) return;
        LivingEntity attacker = event.getEntity();
        if (attacker.level().isClientSide) return;

        SummonEntity summon = SummonEntity.findLinkedSummon(player);
        if (summon == null) return;

        // 骨 Lv10: アンデッドが敵対しない
        if (summon.hasAbility(AbilityId.UNDEAD_NEUTRAL)
                && attacker.getType().is(EntityTypeTags.UNDEAD)) {
            event.setCanceled(true);
            return;
        }

        // 骨 Lv15: 隠密（6ブロックより遠い敵には気づかれない）
        if (summon.hasAbility(AbilityId.STEALTH)
                && attacker instanceof Enemy
                && attacker.distanceToSqr(player) > 36.0D) {
            event.setCanceled(true);
        }
    }
}
