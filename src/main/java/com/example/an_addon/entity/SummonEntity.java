package com.example.an_addon.entity;

import com.example.an_addon.summon.SummonBase;
import com.example.an_addon.summon.SummonData;
import com.example.an_addon.summon.SummonElement;
import com.example.an_addon.summon.ability.AbilityId;
import com.example.an_addon.summon.ability.AbilityTable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SummonEntity extends PathfinderMob implements OwnableEntity, PlayerRideableJumping {

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(SummonEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_POSSESSING =
            SynchedEntityData.defineId(SummonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(SummonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TIER =
            SynchedEntityData.defineId(SummonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_BASE =
            SynchedEntityData.defineId(SummonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ELEMENT =
            SynchedEntityData.defineId(SummonEntity.class, EntityDataSerializers.STRING);

    private final SummonProgression progression = new SummonProgression(this);
    private final SummonPossessController possess = new SummonPossessController(this);
    private final SummonRideController ride = new SummonRideController(this);
    private final SummonAbilityEffects effects = new SummonAbilityEffects(this);
    private final SummonPersistence persistence = new SummonPersistence(this);

    private SummonData summonData = SummonData.DEFAULT;

    public SummonEntity(EntityType<? extends SummonEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.JUMP_STRENGTH, 0.7D)
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_POSSESSING, false);
        builder.define(DATA_LEVEL, 1);
        builder.define(DATA_TIER, 1);
        builder.define(DATA_BASE, SummonBase.SLIME.getId());
        builder.define(DATA_ELEMENT, SummonElement.NONE.getId());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ================= データ =================

    public SummonData getSummonData() {
        return summonData;
    }

    public void setSummonData(SummonData data) {
        this.summonData = data;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_POSSESSING, data.possessing());
            this.entityData.set(DATA_LEVEL, data.level());
            this.entityData.set(DATA_TIER, data.tier());
            this.entityData.set(DATA_BASE, data.base().getId());
            this.entityData.set(DATA_ELEMENT, data.element().getId());
        }
        progression.applyStats();
        possess.applyState();
        progression.updateDisplayName();
    }

    public int getSyncedLevel() {
        return this.entityData.get(DATA_LEVEL);
    }

    public int getSyncedTier() {
        return this.entityData.get(DATA_TIER);
    }

    public SummonBase getSyncedBase() {
        return SummonBase.byId(this.entityData.get(DATA_BASE));
    }

    public String getSyncedElementId() {
        return this.entityData.get(DATA_ELEMENT);
    }

    public boolean isPossessing() {
        return this.entityData.get(DATA_POSSESSING);
    }

    /** 能力が解放済みか（両サイドで判定可能） */
    public boolean hasAbility(AbilityId ability) {
        return AbilityTable.has(getSyncedBase(), getSyncedLevel(), ability);
    }

    public boolean canRideNow() {
        return hasAbility(AbilityId.RIDE);
    }

    public void grantExp(int amount) {
        progression.grantExp(amount);
    }

    public boolean syncToCard() {
        return progression.syncToCard();
    }

    /** プレイヤーと連結中（騎乗中または憑依中）か */
    public boolean isLinkedTo(Player player) {
        if (!isOwner(player)) return false;
        return isPossessing() || player.getVehicle() == this;
    }

    /** そのプレイヤーに連結している召喚獣を探す。無ければ null */
    @Nullable
    public static SummonEntity findLinkedSummon(Player player) {
        if (player.getVehicle() instanceof SummonEntity ridden && ridden.isOwner(player)) {
            return ridden;
        }
        List<SummonEntity> list = player.level().getEntitiesOfClass(
                SummonEntity.class, player.getBoundingBox().inflate(2.0D),
                s -> s.isPossessing() && s.isOwner(player));
        return list.isEmpty() ? null : list.get(0);
    }

    // ================= 所有者 =================

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER, Optional.ofNullable(uuid));
    }

    public boolean isOwner(Player player) {
        UUID owner = getOwnerUUID();
        return owner != null && owner.equals(player.getUUID());
    }

    @Nullable
    public LivingEntity getOwnerEntity() {
        UUID owner = getOwnerUUID();
        if (owner == null) return null;
        Player player = this.level().getPlayerByUUID(owner);
        if (player != null) return player;
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(owner) instanceof LivingEntity living ? living : null;
        }
        return null;
    }

    // ================= 憑依 =================

    public boolean togglePossession(Player player) {
        return possess.toggle(player);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_POSSESSING.equals(key) && possess != null) {
            possess.applyState();
        }
    }

    // ================= 流体 =================

    @Override
    public boolean canStandOnFluid(FluidState state) {
        if (hasAbility(AbilityId.WATER_WALK) && state.is(FluidTags.WATER)) {
            return true;
        }
        return super.canStandOnFluid(state);
    }

    @Override
    public boolean fireImmune() {
        return hasAbility(AbilityId.MAGMA_RESIST) || super.fireImmune();
    }

    // ================= 当たり判定 =================

    @Override
    public boolean isPushable() {
        return !isPossessing();
    }

    @Override
    protected void pushEntities() {
        if (!isPossessing()) super.pushEntities();
    }

    @Override
    public void push(Entity entity) {
        if (!isPossessing()) super.push(entity);
    }

    @Override
    protected void doPush(Entity entity) {
        if (!isPossessing()) super.doPush(entity);
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isPossessing() && super.canBeCollidedWith();
    }

    @Override
    public boolean isPickable() {
        return !isPossessing() && super.isPickable();
    }

    @Override
    public boolean isAttackable() {
        return !isPossessing() && super.isAttackable();
    }

    // ================= 騎乗 =================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwner(player)) return InteractionResult.PASS;
        if (!canRideNow()) {
            if (!this.level().isClientSide) {
                player.displayClientMessage(Component.literal(
                        getSyncedBase().isRideable()
                                ? "Lv" + SummonData.RELEASE_LEVEL + " で騎乗が解放される"
                                : getSyncedBase().getDisplayName() + " には騎乗できない"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        if (this.getFirstPassenger() == null) {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (this.getFirstPassenger() instanceof Player player && isOwner(player)) {
            return player;
        }
        return null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        ride.tickRidden(player, travelVector);
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 deltaIn) {
        return ride.getRiddenInput(player, deltaIn);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return ride.getRiddenSpeed(player);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0D, dimensions.height() * 0.85D, 0.0D);
    }

    @Override
    public void onPlayerJump(int jumpPower) {
        ride.onPlayerJump(jumpPower);
    }

    @Override
    public boolean canJump() {
        return canRideNow();
    }

    @Override
    public void handleStartJump(int jumpPower) {
        ride.handleStartJump(jumpPower);
    }

    @Override
    public void handleStopJump() {
    }

    /** 騎乗中の向き同期。Entity#setRot が protected なため本体側で公開する */
    public void applyRiddenRotation(float yRot, float xRot) {
        this.setRot(yRot, xRot);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    // ================= 挙動 =================

    @Override
    public void tick() {
        super.tick();
        ride.tickGroundState();

        LivingEntity owner = getOwnerEntity();

        if (isPossessing()) {
            if (owner == null) {
                if (!this.level().isClientSide) {
                    setSummonData(summonData.withPossessing(false));
                }
                return;
            }
            possess.tick(owner);
            if (!this.level().isClientSide && owner instanceof Player player) {
                effects.tickLinkedBuffs(player);
            }
            return;
        }

        if (this.level().isClientSide) return;

        if (owner instanceof Player player && player.getVehicle() == this) {
            effects.tickLinkedBuffs(player);
        }

        if (owner != null && this.getFirstPassenger() == null && this.tickCount % 20 == 0) {
            double dist = this.distanceToSqr(owner);
            if (dist > 24 * 24) {
                this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                this.getNavigation().stop();
            } else if (dist > 10 * 10 && this.getTarget() == null) {
                this.getNavigation().moveTo(owner, 1.0D);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        UUID owner = getOwnerUUID();
        if (source.getEntity() != null && owner != null && source.getEntity().getUUID().equals(owner)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void remove(RemovalReason reason) {
        possess.clearLight();
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        possess.clearLight();
        progression.onDeath();
        super.die(source);
    }

    // ================= 保存 =================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        persistence.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        persistence.load(tag);
    }
}
