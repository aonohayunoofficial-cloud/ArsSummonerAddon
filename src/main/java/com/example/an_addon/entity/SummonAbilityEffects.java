package com.example.an_addon.entity;

import com.example.an_addon.summon.ability.AbilityId;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 騎乗中・憑依中に所有者へ与える恩恵を担当する */
public class SummonAbilityEffects {

    private static final double SCENT_RANGE = 14.0D;

    private final SummonEntity summon;

    SummonAbilityEffects(SummonEntity summon) {
        this.summon = summon;
    }

    public void tickLinkedBuffs(Player owner) {
        if (summon.hasAbility(AbilityId.MAGMA_RESIST) || summon.hasAbility(AbilityId.LAVA_SWIM)) {
            owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
        }
        if (summon.hasAbility(AbilityId.HOVER)) {
            owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
            owner.resetFallDistance();
        }
        if (summon.hasAbility(AbilityId.DARKNESS_IMMUNE)) {
            owner.removeEffect(MobEffects.BLINDNESS);
            owner.removeEffect(MobEffects.DARKNESS);
            owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, true));
        }
        if (summon.hasAbility(AbilityId.SCENT_DETECT) && summon.tickCount % 20 == 0) {
            for (LivingEntity target : summon.level().getEntitiesOfClass(
                    LivingEntity.class, owner.getBoundingBox().inflate(SCENT_RANGE),
                    e -> e.isAlive() && e != owner && !(e instanceof SummonEntity))) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
            }
        }
    }
}
