package com.example.an_addon.entity;

import com.example.an_addon.config.AddonConfig;
import com.example.an_addon.item.ContractCardItem;
import com.example.an_addon.registry.ModRegistry;
import com.example.an_addon.summon.SummonBase;
import com.example.an_addon.summon.SummonData;
import com.example.an_addon.summon.SummonElement;
import com.example.an_addon.summon.ability.AbilityId;
import com.example.an_addon.summon.ability.AbilityTable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SummonEntity extends PathfinderMob implements OwnableEntity, PlayerRideableJumping {

    private static final int POSSESS_LIGHT_LEVEL = 8;
    private static final double POSSESS_OUTLINE_RANGE = 8.0D;
    private static final double SCENT_RANGE = 14.0D;

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

    private SummonData summonData = SummonData.DEFAULT;

    @Nullable
    private BlockPos lightPos;

    private float playerJumpPendingScale;
    private boolean isJumping;

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
        applyStats();
        applyPossessState();
        updateDisplayName();
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

    private void applyStats() {
        int lv = summonData.level();
        // ティアは倍率で効かせる。T1 1.00 / T2 1.25 / T3 1.50 / T4 1.75 / T5 2.00
        double tierMul = 1.0D + (summonData.tier() - 1) * 0.25D;
        double hp = (20.0D + (lv - 1) * 1.5D) * tierMul;
        double atk = (3.0D + (lv - 1) * 0.35D) * tierMul;
        double jump = hasAbility(AbilityId.HIGH_JUMP) ? 1.05D : 0.7D;

        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(atk);
        }
        if (this.getAttribute(Attributes.JUMP_STRENGTH) != null) {
            this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jump);
        }
        if (this.getHealth() > hp) {
            this.setHealth((float) hp);
        }
    }

    private void updateDisplayName() {
        this.setCustomName(Component.literal(
                summonData.base().getDisplayName()
                        + "（" + summonData.element().getDisplayName() + "）"
                        + " T" + summonData.tier()
                        + " Lv" + summonData.level()));
        this.setCustomNameVisible(!isPossessing());
    }

    public void grantExp(int amount) {
        if (this.level().isClientSide || amount <= 0) return;
        int beforeLv = summonData.level();
        setSummonData(summonData.grantExp(amount));
        syncToCard();
        if (!(getOwnerEntity() instanceof Player player)) return;
        int afterLv = summonData.level();

        if (afterLv > beforeLv) {
            if (AddonConfig.SHOW_LEVEL_UP.get()) {
                player.displayClientMessage(Component.literal(
                        "★ " + summonData.base().getDisplayName() + " が Lv" + afterLv + " になった"), false);
            }
            if (AddonConfig.SHOW_ABILITY_UNLOCK.get()) {
                for (AbilityTable.Entry e : AbilityTable.entriesFor(summonData.base())) {
                    if (e.level() > beforeLv && e.level() <= afterLv) {
                        player.displayClientMessage(Component.literal(
                                "【解放】" + e.ability().getDisplayName()), false);
                    }
                }
            }
        } else if (AddonConfig.SHOW_EXP_GAIN.get()) {
            player.displayClientMessage(Component.literal(
                    "+" + amount + " EXP (" + summonData.exp() + "/" + summonData.expToNext() + ")"), true);
        }
    }

    public boolean syncToCard() {
        if (!(getOwnerEntity() instanceof Player player)) return false;
        ItemStack card = findCard(player);
        if (card.isEmpty()) return false;
        card.set(ModRegistry.SUMMON_DATA.get(), this.summonData);
        return true;
    }

    private ItemStack findCard(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!(stack.getItem() instanceof ContractCardItem)) continue;
            SummonData cardData = stack.get(ModRegistry.SUMMON_DATA.get());
            if (cardData != null && cardData.entityId().isPresent()
                    && cardData.entityId().get().equals(this.getUUID())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
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
        if (this.level().isClientSide) return false;
        if (!summonData.canPossess()) {
            player.displayClientMessage(Component.literal(
                    summonData.base().isPossessable()
                            ? "Lv" + SummonData.RELEASE_LEVEL + " で憑依が解放される"
                            : summonData.base().getDisplayName() + " は憑依できない"), true);
            return false;
        }
        boolean next = !summonData.possessing();
        if (next) {
            this.ejectPassengers();
        }
        setSummonData(summonData.withPossessing(next));
        syncToCard();
        if (AddonConfig.SHOW_STATE_MESSAGE.get()) {
            player.displayClientMessage(Component.literal(
                    next ? summonData.base().getDisplayName() + " が憑依した"
                            : summonData.base().getDisplayName() + " の憑依を解いた"), true);
        }
        this.level().playSound(null, this.blockPosition(),
                next ? SoundEvents.ILLUSIONER_CAST_SPELL : SoundEvents.ILLUSIONER_MIRROR_MOVE,
                this.getSoundSource(), 0.8F, next ? 1.4F : 0.8F);
        return true;
    }

    private void applyPossessState() {
        boolean p = isPossessing();
        this.setInvisible(p);
        this.setSilent(p);
        this.noPhysics = p;
        if (!this.level().isClientSide) {
            this.setNoAi(p);
            this.setInvulnerable(p);
        }
        if (!p) {
            clearLight();
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_POSSESSING.equals(key)) {
            applyPossessState();
        }
    }

    private void tickPossession(LivingEntity owner) {
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setDeltaMovement(Vec3.ZERO);
        this.setOldPosAndRot();
        this.setYRot(owner.getYRot());
        this.setYHeadRot(owner.getYRot());
        this.setYBodyRot(owner.getYRot());

        if (this.level().isClientSide) return;

        if (summonData.base() == SummonBase.LAMP) {
            tickLight(owner);
        }
        if (summonData.base() == SummonBase.BONE) {
            tickOutline(owner);
        }
    }

    private void tickLight(LivingEntity owner) {
        BlockPos target = BlockPos.containing(owner.getX(), owner.getEyeY(), owner.getZ());
        if (target.equals(lightPos)) return;
        clearLight();
        if (this.level().getBlockState(target).isAir()) {
            this.level().setBlockAndUpdate(target, Blocks.LIGHT.defaultBlockState()
                    .setValue(LightBlock.LEVEL, AddonConfig.POSSESS_LIGHT_LEVEL.get()));
            lightPos = target;
        }
    }

    private void clearLight() {
        if (lightPos == null || this.level().isClientSide) {
            lightPos = null;
            return;
        }
        if (this.level().getBlockState(lightPos).is(Blocks.LIGHT)) {
            this.level().setBlockAndUpdate(lightPos, Blocks.AIR.defaultBlockState());
        }
        lightPos = null;
    }

    private void tickOutline(LivingEntity owner) {
        if (this.tickCount % 20 != 0) return;
        for (LivingEntity target : this.level().getEntitiesOfClass(
                LivingEntity.class, owner.getBoundingBox().inflate(POSSESS_OUTLINE_RANGE),
                e -> e instanceof Enemy && e.isAlive())) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
        }
    }

    // ================= 連結中の恩恵 =================

    private void tickLinkedBuffs(Player owner) {
        if (hasAbility(AbilityId.MAGMA_RESIST) || hasAbility(AbilityId.LAVA_SWIM)) {
            owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
        }
        if (hasAbility(AbilityId.HOVER)) {
            owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
            owner.resetFallDistance();
        }
        if (hasAbility(AbilityId.DARKNESS_IMMUNE)) {
            owner.removeEffect(MobEffects.BLINDNESS);
            owner.removeEffect(MobEffects.DARKNESS);
            owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, true));
        }
        if (hasAbility(AbilityId.SCENT_DETECT) && this.tickCount % 20 == 0) {
            for (LivingEntity target : this.level().getEntitiesOfClass(
                    LivingEntity.class, owner.getBoundingBox().inflate(SCENT_RANGE),
                    e -> e.isAlive() && e != owner && !(e instanceof SummonEntity))) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
            }
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
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();

        // 壁張り付き: 壁に当たりながら前進すると登る
        if (hasAbility(AbilityId.WALL_CLING) && this.horizontalCollision && travelVector.z > 0.0D) {
            Vec3 move = this.getDeltaMovement();
            this.setDeltaMovement(move.x, 0.22D, move.z);
            this.resetFallDistance();
            this.isJumping = false;
        }

        // 溶岩泳ぎ: 溶岩の中で沈まず操作できる
        if (hasAbility(AbilityId.LAVA_SWIM) && this.isInLava()) {
            Vec3 move = this.getDeltaMovement();
            this.setDeltaMovement(move.x * 1.6D, Math.max(move.y, 0.05D), move.z * 1.6D);
            this.clearFire();
        }

        if (this.onGround() && this.playerJumpPendingScale > 0.0F && !this.isJumping) {
            double jump = this.getAttributeValue(Attributes.JUMP_STRENGTH) * this.playerJumpPendingScale;
            Vec3 move = this.getDeltaMovement();
            this.setDeltaMovement(move.x, jump, move.z);
            this.isJumping = true;
            this.hasImpulse = true;
            if (travelVector.z > 0.0D) {
                float sin = Mth.sin(this.getYRot() * ((float) Math.PI / 180F));
                float cos = Mth.cos(this.getYRot() * ((float) Math.PI / 180F));
                this.setDeltaMovement(this.getDeltaMovement().add(
                        -0.4F * sin * this.playerJumpPendingScale, 0.0D,
                        0.4F * cos * this.playerJumpPendingScale));
            }
            this.playerJumpPendingScale = 0.0F;
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 deltaIn) {
        float strafe = player.xxa * 0.5F;
        float forward = player.zza;
        if (forward <= 0.0F) forward *= 0.4F;
        return new Vec3(strafe, 0.0D, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        float lvBonus = 1.0F + Math.min(getSyncedLevel(), SummonData.LEVEL_CAP) * 0.01F;
        float baseFactor = getSyncedBase().getRideSpeedFactor();
        double configMul = AddonConfig.RIDE_SPEED_MULTIPLIER.get();
        return (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED)
                * baseFactor * lvBonus * configMul);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0D, dimensions.height() * 0.85D, 0.0D);
    }

    @Override
    public void onPlayerJump(int jumpPower) {
        if (jumpPower < 0) jumpPower = 0;
        this.playerJumpPendingScale = jumpPower >= 90 ? 1.0F : 0.4F + 0.4F * jumpPower / 90.0F;
    }

    @Override
    public boolean canJump() {
        return canRideNow();
    }

    @Override
    public void handleStartJump(int jumpPower) {
        this.playSound(SoundEvents.SLIME_JUMP, 0.6F, 1.2F);
    }

    @Override
    public void handleStopJump() {
    }

    // ================= 挙動 =================

    @Override
    public void tick() {
        super.tick();

        if (this.onGround()) {
            this.isJumping = false;
        }

        LivingEntity owner = getOwnerEntity();

        if (isPossessing()) {
            if (owner == null) {
                if (!this.level().isClientSide) {
                    setSummonData(summonData.withPossessing(false));
                }
                return;
            }
            tickPossession(owner);
            if (!this.level().isClientSide && owner instanceof Player player) {
                tickLinkedBuffs(player);
            }
            return;
        }

        if (this.level().isClientSide) return;

        if (owner instanceof Player player && player.getVehicle() == this) {
            tickLinkedBuffs(player);
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
        clearLight();
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        clearLight();
        if (!this.level().isClientSide && getOwnerEntity() instanceof Player player) {
            ItemStack card = findCard(player);
            if (!card.isEmpty()) {
                card.set(ModRegistry.SUMMON_DATA.get(),
                        summonData.withRelationship(summonData.relationship() - 10)
                                .withPossessing(false)
                                .withEntity(Optional.empty()));
            }
            if (AddonConfig.SHOW_STATE_MESSAGE.get()) {
                player.displayClientMessage(Component.literal(
                        summonData.base().getDisplayName() + " が倒れた（関係値 -10）"), true);
            }
        }
        super.die(source);
    }

    // ================= 保存 =================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        SummonData.CODEC.encodeStart(NbtOps.INSTANCE, summonData)
                .resultOrPartial(err -> System.err.println("SummonData save failed: " + err))
                .ifPresent(t -> tag.put("SummonData", t));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
        }
        if (tag.contains("SummonData")) {
            Tag data = tag.get("SummonData");
            SummonData.CODEC.parse(NbtOps.INSTANCE, data)
                    .resultOrPartial(err -> System.err.println("SummonData load failed: " + err))
                    .ifPresent(this::setSummonData);
        }
    }
}
