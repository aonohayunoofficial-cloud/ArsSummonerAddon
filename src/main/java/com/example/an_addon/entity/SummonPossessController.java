package com.example.an_addon.entity;

import com.example.an_addon.config.AddonConfig;
import com.example.an_addon.summon.SummonBase;
import com.example.an_addon.summon.SummonData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** 憑依の切替・状態適用・所有者追従・灯の光源・骨の輪郭表示を担当する */
public class SummonPossessController {

    private static final double OUTLINE_RANGE = 8.0D;

    private final SummonEntity summon;

    @Nullable
    private BlockPos lightPos;

    SummonPossessController(SummonEntity summon) {
        this.summon = summon;
    }

    public boolean toggle(Player player) {
        if (summon.level().isClientSide) return false;
        SummonData data = summon.getSummonData();

        if (!data.canPossess()) {
            player.displayClientMessage(Component.literal(
                    data.base().isPossessable()
                            ? "Lv" + SummonData.RELEASE_LEVEL + " で憑依が解放される"
                            : data.base().getDisplayName() + " は憑依できない"), true);
            return false;
        }

        boolean next = !data.possessing();
        if (next) {
            summon.ejectPassengers();
        }
        summon.setSummonData(data.withPossessing(next));
        summon.syncToCard();

        if (AddonConfig.SHOW_STATE_MESSAGE.get()) {
            player.displayClientMessage(Component.literal(
                    next ? data.base().getDisplayName() + " が憑依した"
                            : data.base().getDisplayName() + " の憑依を解いた"), true);
        }
        summon.level().playSound(null, summon.blockPosition(),
                next ? SoundEvents.ILLUSIONER_CAST_SPELL : SoundEvents.ILLUSIONER_MIRROR_MOVE,
                summon.getSoundSource(), 0.8F, next ? 1.4F : 0.8F);
        return true;
    }

    /** 憑依フラグに応じて可視・当たり判定・AI を切り替える */
    public void applyState() {
        boolean p = summon.isPossessing();
        summon.setInvisible(p);
        summon.setSilent(p);
        summon.noPhysics = p;
        if (!summon.level().isClientSide) {
            summon.setNoAi(p);
            summon.setInvulnerable(p);
        }
        if (!p) {
            clearLight();
        }
    }

    public void tick(LivingEntity owner) {
        summon.setPos(owner.getX(), owner.getY(), owner.getZ());
        summon.setDeltaMovement(Vec3.ZERO);
        summon.setOldPosAndRot();
        summon.setYRot(owner.getYRot());
        summon.setYHeadRot(owner.getYRot());
        summon.setYBodyRot(owner.getYRot());

        if (summon.level().isClientSide) return;

        SummonBase base = summon.getSummonData().base();
        if (base == SummonBase.LAMP) {
            tickLight(owner);
        }
        if (base == SummonBase.BONE) {
            tickOutline(owner);
        }
    }

    private void tickLight(LivingEntity owner) {
        BlockPos target = BlockPos.containing(owner.getX(), owner.getEyeY(), owner.getZ());
        if (target.equals(lightPos)) return;
        clearLight();
        if (summon.level().getBlockState(target).isAir()) {
            summon.level().setBlockAndUpdate(target, Blocks.LIGHT.defaultBlockState()
                    .setValue(LightBlock.LEVEL, AddonConfig.POSSESS_LIGHT_LEVEL.get()));
            lightPos = target;
        }
    }

    public void clearLight() {
        if (lightPos == null || summon.level().isClientSide) {
            lightPos = null;
            return;
        }
        if (summon.level().getBlockState(lightPos).is(Blocks.LIGHT)) {
            summon.level().setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        }
        lightPos = null;
    }

    private void tickOutline(LivingEntity owner) {
        if (summon.tickCount % 20 != 0) return;
        for (LivingEntity target : summon.level().getEntitiesOfClass(
                LivingEntity.class, owner.getBoundingBox().inflate(OUTLINE_RANGE),
                e -> e instanceof Enemy && e.isAlive())) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
        }
    }
}
