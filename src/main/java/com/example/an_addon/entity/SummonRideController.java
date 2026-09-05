package com.example.an_addon.entity;

import com.example.an_addon.config.AddonConfig;
import com.example.an_addon.summon.SummonData;
import com.example.an_addon.summon.ability.AbilityId;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 騎乗中の操作・速度・ジャンプ・壁張り付き・溶岩泳ぎを担当する */
public class SummonRideController {

    private final SummonEntity summon;

    private float playerJumpPendingScale;
    private boolean isJumping;

    SummonRideController(SummonEntity summon) {
        this.summon = summon;
    }

    /** 接地したらジャンプ中フラグを解除する。毎 tick 呼ぶ */
    public void tickGroundState() {
        if (summon.onGround()) {
            isJumping = false;
        }
    }

    public void tickRidden(Player player, Vec3 travelVector) {
        summon.applyRiddenRotation(player.getYRot(), player.getXRot() * 0.5F);

        // 壁張り付き: 壁に当たりながら前進すると登る
        if (summon.hasAbility(AbilityId.WALL_CLING)
                && summon.horizontalCollision && travelVector.z > 0.0D) {
            Vec3 move = summon.getDeltaMovement();
            summon.setDeltaMovement(move.x, 0.22D, move.z);
            summon.resetFallDistance();
            this.isJumping = false;
        }

        // 溶岩泳ぎ: 溶岩の中で沈まず操作できる
        if (summon.hasAbility(AbilityId.LAVA_SWIM) && summon.isInLava()) {
            Vec3 move = summon.getDeltaMovement();
            summon.setDeltaMovement(move.x * 1.6D, Math.max(move.y, 0.05D), move.z * 1.6D);
            summon.clearFire();
        }

        if (summon.onGround() && this.playerJumpPendingScale > 0.0F && !this.isJumping) {
            double jump = summon.getAttributeValue(Attributes.JUMP_STRENGTH) * this.playerJumpPendingScale;
            Vec3 move = summon.getDeltaMovement();
            summon.setDeltaMovement(move.x, jump, move.z);
            this.isJumping = true;
            summon.hasImpulse = true;
            if (travelVector.z > 0.0D) {
                float sin = Mth.sin(summon.getYRot() * ((float) Math.PI / 180F));
                float cos = Mth.cos(summon.getYRot() * ((float) Math.PI / 180F));
                summon.setDeltaMovement(summon.getDeltaMovement().add(
                        -0.4F * sin * this.playerJumpPendingScale, 0.0D,
                        0.4F * cos * this.playerJumpPendingScale));
            }
            this.playerJumpPendingScale = 0.0F;
        }
    }

    public Vec3 getRiddenInput(Player player, Vec3 deltaIn) {
        float strafe = player.xxa * 0.5F;
        float forward = player.zza;
        if (forward <= 0.0F) forward *= 0.4F;
        return new Vec3(strafe, 0.0D, forward);
    }

    /** ベース係数 × レベル補正 × コンフィグ倍率。獣が速く岩が遅い */
    public float getRiddenSpeed(Player player) {
        float lvBonus = 1.0F + Math.min(summon.getSyncedLevel(), SummonData.LEVEL_CAP) * 0.01F;
        float baseFactor = summon.getSyncedBase().getRideSpeedFactor();
        double configMul = AddonConfig.RIDE_SPEED_MULTIPLIER.get();
        return (float) (summon.getAttributeValue(Attributes.MOVEMENT_SPEED)
                * baseFactor * lvBonus * configMul);
    }

    public void onPlayerJump(int jumpPower) {
        if (jumpPower < 0) jumpPower = 0;
        this.playerJumpPendingScale = jumpPower >= 90 ? 1.0F : 0.4F + 0.4F * jumpPower / 90.0F;
    }

    public void handleStartJump(int jumpPower) {
        summon.playSound(SoundEvents.SLIME_JUMP, 0.6F, 1.2F);
    }
}
