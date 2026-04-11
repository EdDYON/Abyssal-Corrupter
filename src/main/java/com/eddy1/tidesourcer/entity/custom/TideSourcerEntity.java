package com.eddy1.tidesourcer.entity.custom;

import com.eddy1.tidesourcer.config.AbyssalConfig;
import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.ai.SunkenTitanCombatGoal;
import com.eddy1.tidesourcer.entity.ai.SunkenTitanCombatManager;
import com.eddy1.tidesourcer.entity.ai.SunkenTitanSpeechManager;
import com.eddy1.tidesourcer.entity.ai.module.SkillCastHelper;
import com.eddy1.tidesourcer.entity.ai.module.epic.EchoingPulseDomain;
import com.eddy1.tidesourcer.entity.ai.module.terrain.AbyssalTrench;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class TideSourcerEntity extends Monster implements GeoEntity {
    public static final int FOLLOW_UP_NONE = 0;
    public static final int FOLLOW_UP_CLOSE = 1;
    public static final int FOLLOW_UP_CHASE = 2;
    public static final int FOLLOW_UP_RANGED = 3;
    public static final int FOLLOW_UP_EXECUTE = 4;

    private static final EntityDataAccessor<Integer> DATA_ATTACK_STATE = SynchedEntityData.defineId(TideSourcerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TICK = SynchedEntityData.defineId(TideSourcerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(TideSourcerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ENTRANCE_TICKS = SynchedEntityData.defineId(TideSourcerEntity.class, EntityDataSerializers.INT);

    private static final Set<TideSourcerEntity> ACTIVE_INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.abyssal_corrupter.abyssal_corrupter"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_6
    ).setDarkenScreen(true);

    public int attackState = 0;
    public int attackVariant = 1;
    public int attackTick = 0;

    public int cdGlobalEpic = 200;

    public int cdSlash = 0;
    public int cdCombo = 0;
    public int cdWave = 0;

    public int cdLeap = 0;
    public int cdEruption = 0;
    public int cdSkyfall = 0;

    public int cdVortex = 0;
    public int cdRepulsion = 0;
    public int cdPrison = 0;

    public int cdHook = 0;
    public int cdBreath = 0;
    public int cdRay = 0;

    public int cdDash = 0;
    public int cdTaunt = 0;

    public int cdDomain = 0;
    public int cdMirage = 0;
    public int cdArmory = 0;
    public int cdNova = 0;
    public int cdSingularity = 0;

    public double geyserX;
    public double geyserY;
    public double geyserZ;

    public Map<BlockPos, BlockState> savedEchoDomainBlocks = new HashMap<>();

    public boolean isClone = false;
    public TideSourcerEntity mainBoss = null;
    public boolean mirageFailed = false;
    public boolean mirageSuccess = false;
    public boolean mirageFinalShotFired = false;
    public List<TideSourcerEntity> activeClones = new ArrayList<>();

    public int armoryActiveTick = 0;
    public List<AbstractArrow> activeArmoryWeapons = new ArrayList<>();

    public int echoDomainActiveTick = 0;
    public int echoDomainConfusionTick = 0;
    public Vec3 echoDomainCenter = null;

    public int singularityActiveTick = 0;
    public Vec3 singularityPos = null;

    public double coralBaseAngle = 0.0D;
    public Map<UUID, Integer> coralHitTicks = new HashMap<>();
    public boolean manualTestMode = false;

    public UUID trackedTargetId = null;
    public double trackedTargetDistanceSqr = -1.0D;
    public int targetRetreatTicks = 0;
    public int targetApproachTicks = 0;
    public int targetStrafeTicks = 0;
    public int targetStraightRunTicks = 0;
    public int targetCloseTicks = 0;
    public int targetFarTicks = 0;
    public int targetNoSightTicks = 0;
    public int targetAirTicks = 0;
    private int pursuitAssistCooldown = 0;
    private int pursuitRecoveryCooldown = 0;
    private int pursuitStuckTicks = 0;
    private Vec3 lastPursuitSamplePos = null;

    public int followUpIntent = FOLLOW_UP_NONE;
    public int followUpTicks = 0;
    public int lastAttackStateUsed = 0;
    public int lastAttackVariantUsed = 0;
    public int repeatedAttackCount = 0;

    public boolean phase90 = false;
    public boolean phase80 = false;
    public boolean phase70 = false;
    public boolean phase60 = false;
    public int pendingPhaseEpic = 0;
    private int entranceTicks = 0;

    public TideSourcerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        ACTIVE_INSTANCES.add(this);
        this.cdDash = this.scaleCooldown(20);
        this.cdRay = this.scaleCooldown(60);
        this.cdSkyfall = this.scaleCooldown(80);
        this.cdMirage = this.scaleCooldown(100);
        this.cdArmory = this.scaleCooldown(160);
        this.cdNova = this.scaleCooldown(120);
        this.cdSingularity = this.scaleCooldown(140);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, AbyssalConfig.scaledHealth(1800.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, AbyssalConfig.scaledDamage(32.0F))
                .add(Attributes.ATTACK_KNOCKBACK, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public static List<TideSourcerEntity> getActiveInstances(Level level) {
        List<TideSourcerEntity> matches = new ArrayList<>();
        for (TideSourcerEntity entity : ACTIVE_INSTANCES) {
            if (entity == null || entity.level() != level || entity.isRemoved()) {
                continue;
            }
            matches.add(entity);
        }
        return matches;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_STATE, 0);
        builder.define(DATA_ATTACK_TICK, 0);
        builder.define(DATA_PHASE_TWO, false);
        builder.define(DATA_ENTRANCE_TICKS, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("IsClone", this.isClone);
        compound.putInt("EntranceTicks", this.entranceTicks);
        compound.putBoolean("Phase60", this.phase60);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.isClone = compound.getBoolean("IsClone");
        this.entranceTicks = compound.getInt("EntranceTicks");
        this.phase60 = compound.getBoolean("Phase60");
        if (this.phase60) {
            this.phase70 = true;
            this.phase80 = true;
            this.phase90 = true;
        }
        this.syncCombatData();
    }

    @Override
    protected void registerGoals() {
        if (!this.isClone) {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(1, new SunkenTitanCombatGoal(this));
            this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
            this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
            this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        if (this.entranceTicks > 0) {
            this.tickEntrance((ServerLevel) this.level());
            this.syncCombatData();
            return;
        }

        if (!this.isClone && this.hasTemporaryEncounterState() && this.isEncounterTargetInvalid()) {
            this.interruptEncounter();
        }

        if (this.isClone) {
            if (this.mainBoss == null || !this.mainBoss.isAlive() || this.mainBoss.attackState != 7) {
                this.discard();
                return;
            }
            if (this.tickCount % 3 == 0) {
                AbyssalEffects.spawnInfectionCloud((ServerLevel) this.level(), this.position().add(0.0D, 1.0D, 0.0D), 0.12D, 0.25D);
            }
            this.syncCombatData();
            return;
        }

        if (this.pursuitRecoveryCooldown > 0) {
            this.pursuitRecoveryCooldown--;
        }
        if (this.pursuitAssistCooldown > 0) {
            this.pursuitAssistCooldown--;
        }

        if (this.echoDomainConfusionTick > 0) {
            this.echoDomainConfusionTick--;
        }

        if (this.echoDomainActiveTick > 0) {
            this.echoDomainActiveTick--;
            if (this.level() instanceof ServerLevel sl) {
                EchoingPulseDomain.tickActiveDomain(this, sl);
            }
        }

        if (this.singularityActiveTick > 0) {
            this.singularityActiveTick--;
            if (this.level() instanceof ServerLevel sl) {
                AbyssalTrench.tickActiveTrench(this, sl);
            }
        }

        if (this.attackState == 100) {
            this.attackTick++;
            if (this.attackTick == 1) {
                SunkenTitanSpeechManager.announceSkill(this, 100, 1);
                this.playSound(SoundEvents.WITHER_DEATH, 3.0F, 0.5F);
                this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 5.0F, 0.5F);
                this.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0, false, false, false));
                this.setDeltaMovement(0.0D, 0.0D, 0.0D);
            }
            if (this.level() instanceof ServerLevel sl) {
                Vec3 pos = this.position().add(0.0D, 1.0D, 0.0D);
                AbyssalEffects.spawnCharge(sl, pos, 2.0D, 2.0D);
                AbyssalEffects.spawnInfectionCloud(sl, pos, 2.0D, 2.0D);
            }
            if (this.attackTick >= 60) {
                this.removeEffect(MobEffects.LEVITATION);
                int nextEpic = this.pendingPhaseEpic;
                this.attackState = 0;
                this.attackTick = 0;
                this.pendingPhaseEpic = 0;
                if (nextEpic != 0) {
                    this.startEpicAttack(nextEpic);
                }
            }
            this.syncCombatData();
            return;
        }

        if (this.armoryActiveTick > 0) {
            this.armoryActiveTick--;
            if (this.armoryActiveTick == 0) {
                for (AbstractArrow weapon : this.activeArmoryWeapons) {
                    if (weapon != null && weapon.isAlive()) {
                        if (this.level() instanceof ServerLevel sl) {
                            AbyssalEffects.spawnImpact(sl, weapon.position(), 0.3D, 0.3D);
                        }
                        weapon.discard();
                    }
                }
                this.activeArmoryWeapons.clear();
            }
        }

        SunkenTitanCombatManager.handleCooldowns(this);
        if (this.attackState != 0 && this.attackState != 100) {
            this.attackTick++;
            SunkenTitanCombatManager.handleAttacks(this);
        }
        this.syncCombatData();
    }

    public void forceRestoreBlocks() {
        if (this.level() instanceof ServerLevel sl) {
            for (Map.Entry<BlockPos, BlockState> entry : this.savedEchoDomainBlocks.entrySet()) {
                sl.setBlock(entry.getKey(), entry.getValue(), 3);
            }
            this.savedEchoDomainBlocks.clear();
        }
    }

    public boolean hasPersistentEpicActive() {
        return this.echoDomainActiveTick > 0
                || this.singularityActiveTick > 0
                || this.armoryActiveTick > 0;
    }

    public boolean hasTemporaryEncounterState() {
        return (this.attackState >= 6 && this.attackState <= 10)
                || this.echoDomainActiveTick > 0
                || this.singularityActiveTick > 0
                || this.armoryActiveTick > 0
                || !this.savedEchoDomainBlocks.isEmpty()
                || !this.activeClones.isEmpty()
                || !this.activeArmoryWeapons.isEmpty()
                || this.isInvisible();
    }

    public boolean isTrackingTarget(LivingEntity target) {
        return target != null && this.getTarget() == target;
    }

    public void interruptEncounter() {
        this.cleanupMirageClones();
        this.cleanupArmoryWeapons();
        this.forceRestoreBlocks();
        this.echoDomainActiveTick = 0;
        this.echoDomainConfusionTick = 0;
        this.echoDomainCenter = null;
        this.singularityActiveTick = 0;
        this.singularityPos = null;
        this.armoryActiveTick = 0;
        this.mirageFailed = false;
        this.mirageSuccess = false;
        this.mirageFinalShotFired = false;
        this.coralBaseAngle = 0.0D;
        this.coralHitTicks.clear();
        this.resetCombatMemory();
        this.resetPursuitRecoveryState();
        this.setInvisible(false);
        this.attackState = 0;
        this.attackTick = 0;
        this.entranceTicks = 0;
        this.setInvulnerable(false);
        this.syncCombatData();
        this.getNavigation().stop();
    }

    public void cleanupMirageClones() {
        for (TideSourcerEntity clone : this.activeClones) {
            if (clone != null && clone.isAlive()) {
                clone.discard();
            }
        }
        this.activeClones.clear();
        this.mirageFinalShotFired = false;
    }

    public void cleanupArmoryWeapons() {
        if (this.level() instanceof ServerLevel sl) {
            for (AbstractArrow weapon : this.activeArmoryWeapons) {
                if (weapon != null && weapon.isAlive()) {
                    AbyssalEffects.spawnImpact(sl, weapon.position(), 0.3D, 0.3D);
                    weapon.discard();
                }
            }
        } else {
            for (AbstractArrow weapon : this.activeArmoryWeapons) {
                if (weapon != null && weapon.isAlive()) {
                    weapon.discard();
                }
            }
        }
        this.activeArmoryWeapons.clear();
    }

    public void clearCombatCooldowns() {
        this.cdGlobalEpic = 0;
        this.cdSlash = 0;
        this.cdCombo = 0;
        this.cdWave = 0;
        this.cdLeap = 0;
        this.cdEruption = 0;
        this.cdSkyfall = 0;
        this.cdVortex = 0;
        this.cdRepulsion = 0;
        this.cdPrison = 0;
        this.cdHook = 0;
        this.cdBreath = 0;
        this.cdRay = 0;
        this.cdDash = 0;
        this.cdTaunt = 0;
        this.cdDomain = 0;
        this.cdMirage = 0;
        this.cdArmory = 0;
        this.cdNova = 0;
        this.cdSingularity = 0;
        this.pendingPhaseEpic = 0;
        this.resetCombatMemory();
    }

    public void enableManualTestMode() {
        this.manualTestMode = true;
    }

    public void disableManualTestMode() {
        this.manualTestMode = false;
    }

    public boolean isManualTestMode() {
        return this.manualTestMode;
    }

    public void updateCombatMemory(LivingEntity target) {
        if (this.followUpTicks > 0) {
            this.followUpTicks--;
            if (this.followUpTicks == 0) {
                this.followUpIntent = FOLLOW_UP_NONE;
            }
        }

        if (target == null || !target.isAlive() || target.level() != this.level()) {
            this.resetCombatMemory();
            return;
        }

        UUID targetId = target.getUUID();
        if (!targetId.equals(this.trackedTargetId)) {
            this.resetCombatMemory();
            this.trackedTargetId = targetId;
        }

        double distSqr = this.distanceToSqr(target);
        Vec3 toTarget = target.position().subtract(this.position());
        Vec3 horizontalToTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
        Vec3 targetVelocity = target.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);

        double radialDot = 0.0D;
        double strafeDot = 0.0D;
        if (horizontalToTarget.lengthSqr() > 1.0E-4D && targetVelocity.lengthSqr() > 1.0E-4D) {
            Vec3 radial = horizontalToTarget.normalize();
            Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x);
            Vec3 movementDir = targetVelocity.normalize();
            radialDot = movementDir.dot(radial);
            strafeDot = Math.abs(movementDir.dot(tangent));
        }

        this.targetRetreatTicks = updateCombatCounter(this.targetRetreatTicks, radialDot > 0.35D && distSqr > 36.0D, 3, 2, 60);
        this.targetApproachTicks = updateCombatCounter(this.targetApproachTicks, radialDot < -0.30D, 3, 2, 60);
        this.targetStrafeTicks = updateCombatCounter(this.targetStrafeTicks, strafeDot > 0.55D, 2, 1, 60);
        this.targetStraightRunTicks = updateCombatCounter(this.targetStraightRunTicks, radialDot > 0.75D && strafeDot < 0.35D, 3, 2, 60);
        this.targetCloseTicks = updateCombatCounter(this.targetCloseTicks, distSqr <= 36.0D, 2, 3, 60);
        this.targetFarTicks = updateCombatCounter(this.targetFarTicks, distSqr >= 225.0D, 2, 2, 60);
        this.targetNoSightTicks = updateCombatCounter(this.targetNoSightTicks, !this.hasLineOfSight(target), 2, 2, 60);
        this.targetAirTicks = updateCombatCounter(this.targetAirTicks, !target.onGround(), 2, 2, 40);
        this.trackedTargetDistanceSqr = distSqr;
    }

    public void resetCombatMemory() {
        this.trackedTargetId = null;
        this.trackedTargetDistanceSqr = -1.0D;
        this.targetRetreatTicks = 0;
        this.targetApproachTicks = 0;
        this.targetStrafeTicks = 0;
        this.targetStraightRunTicks = 0;
        this.targetCloseTicks = 0;
        this.targetFarTicks = 0;
        this.targetNoSightTicks = 0;
        this.targetAirTicks = 0;
        this.followUpIntent = FOLLOW_UP_NONE;
        this.followUpTicks = 0;
        this.lastAttackStateUsed = 0;
        this.lastAttackVariantUsed = 0;
        this.repeatedAttackCount = 0;
        this.resetPursuitRecoveryState();
    }

    public void setFollowUpIntent(int intent, int ticks) {
        this.followUpIntent = intent;
        this.followUpTicks = ticks;
    }

    @Override
    public boolean shouldBeSaved() {
        return !this.isClone && super.shouldBeSaved();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return this.isClone && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return !this.isClone || super.requiresCustomPersistence();
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide()) {
            this.triggerAnim("attack_controller", "death");
        }
        this.interruptEncounter();
        super.die(damageSource);
    }

    @Override
    protected void tickDeath() {
        if (!this.level().isClientSide() && !this.isClone && this.level() instanceof ServerLevel sl) {
            this.spawnAbyssalDeathEffect(sl);
        }
        super.tickDeath();
    }

    @Override
    public void remove(RemovalReason reason) {
        ACTIVE_INSTANCES.remove(this);
        this.interruptEncounter();
        super.remove(reason);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.attackState == 100 || this.entranceTicks > 0) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL)) {
            return false;
        }
        if (this.isClone) {
            if (this.mainBoss != null && this.mainBoss.isAlive()) {
                this.mainBoss.mirageFailed = true;
            }
            if (this.level() instanceof ServerLevel sl) {
                AbyssalEffects.spawnImpact(sl, this.position().add(0.0D, 1.0D, 0.0D), 0.5D, 0.5D);
            }
            return true;
        }
        if (this.attackState == 7
                && !this.mirageFailed
                && !this.mirageSuccess
                && this.attackTick < com.eddy1.tidesourcer.entity.ai.module.epic.MirageExecution.IDENTIFY_TICKS) {
            this.mirageSuccess = true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        Vec3 vec = this.getDeltaMovement();
        this.setDeltaMovement(vec.x, vec.y + 0.2D, vec.z);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isClone) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (!this.isClone) {
            this.bossEvent.removePlayer(player);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!this.isClone) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            if (this.level() instanceof ServerLevel sl) {
                this.tryRecoverLostTarget(sl);
            }
        }
    }

    public void startAttack(int state, int variant) {
        if (state != 100) {
            if (this.lastAttackStateUsed == state && this.lastAttackVariantUsed == variant) {
                this.repeatedAttackCount = Math.min(this.repeatedAttackCount + 1, 3);
            } else {
                this.repeatedAttackCount = 0;
            }
            this.lastAttackStateUsed = state;
            this.lastAttackVariantUsed = variant;
        }
        this.attackState = state;
        this.attackVariant = variant;
        this.attackTick = 0;
        this.syncCombatData();

        SunkenTitanSpeechManager.announceSkill(this, state, variant);
        this.triggerAttackAnimation(state, variant);
    }

    public void startEpicAttack(int state) {
        this.startAttack(state, 1);
        this.cdGlobalEpic = this.scaleCooldown(420);
        switch (state) {
            case 6 -> this.cdDomain = this.scaleCooldown(1450);
            case 7 -> this.cdMirage = this.scaleCooldown(980);
            case 8 -> this.cdArmory = this.scaleCooldown(1120);
            case 9 -> this.cdNova = this.scaleCooldown(820);
            case 10 -> this.cdSingularity = this.scaleCooldown(960);
            default -> {
            }
        }
    }

    public void resetAttack() {
        if (this.manualTestMode) {
            this.attackState = 0;
            this.attackTick = 0;
            this.syncCombatData();
            return;
        }
        this.applyFollowUpFromFinishedAttack(this.attackState, this.attackVariant);
        this.attackState = 0;
        this.attackTick = 0;
        this.syncCombatData();
    }

    public int scaleCooldown(int baseCooldown) {
        return AbyssalConfig.scaledCooldown(baseCooldown, this.isPhaseTwoActive());
    }

    public void beginEntrance(int ticks) {
        this.entranceTicks = Math.max(0, ticks);
        if (this.entranceTicks > 0) {
            this.attackState = 0;
            this.attackTick = 0;
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            this.setInvulnerable(true);
        }
        this.syncCombatData();
    }

    public boolean isPhaseTwoActive() {
        return this.phase60 || this.getHealth() <= this.getMaxHealth() * 0.60F;
    }

    public int getSyncedAttackState() {
        return this.level().isClientSide() ? this.entityData.get(DATA_ATTACK_STATE) : this.attackState;
    }

    public int getSyncedAttackTick() {
        return this.level().isClientSide() ? this.entityData.get(DATA_ATTACK_TICK) : this.attackTick;
    }

    public boolean isSyncedPhaseTwo() {
        return this.level().isClientSide() ? this.entityData.get(DATA_PHASE_TWO) : this.isPhaseTwoActive();
    }

    public int getSyncedEntranceTicks() {
        return this.level().isClientSide() ? this.entityData.get(DATA_ENTRANCE_TICKS) : this.entranceTicks;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            if (this.isDeadOrDying()) {
                return PlayState.STOP;
            }
            if (this.attackState != 0 && this.attackState != 5 && this.attackState != 100) {
                return PlayState.STOP;
            }
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("attack"))
                .triggerableAnim("attack2", RawAnimation.begin().thenPlay("attack2"))
                .triggerableAnim("attack3", RawAnimation.begin().thenPlay("attack3"))
                .triggerableAnim("attack4", RawAnimation.begin().thenPlay("attack4"))
                .triggerableAnim("attack5", RawAnimation.begin().thenPlay("attack5"))
                .triggerableAnim("attack6", RawAnimation.begin().thenPlay("attack6"))
                .triggerableAnim("attack7", RawAnimation.begin().thenPlay("attack7"))
                .triggerableAnim("death", RawAnimation.begin().thenPlay("death")));
    }

    private void triggerAttackAnimation(int state, int variant) {
        switch (state) {
            case 1 -> this.triggerAnim("attack_controller", "attack");
            case 2, 5 -> this.triggerAnim("attack_controller", "attack2");
            case 3 -> {
                if (variant == 1 || variant == 2) {
                    this.triggerAnim("attack_controller", "attack3");
                } else {
                    this.triggerAnim("attack_controller", "attack6");
                }
            }
            case 4 -> {
                if (variant == 1) {
                    this.triggerAnim("attack_controller", "attack4");
                } else {
                    this.triggerAnim("attack_controller", "attack5");
                }
            }
            case 6 -> this.triggerAnim("attack_controller", "attack6");
            case 7, 11 -> this.triggerAnim("attack_controller", "attack7");
            case 8, 10 -> this.triggerAnim("attack_controller", "attack6");
            case 9 -> this.triggerAnim("attack_controller", "attack5");
            default -> {
            }
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private boolean isEncounterTargetInvalid() {
        LivingEntity target = this.getTarget();
        return target == null || !target.isAlive() || target.isRemoved() || target.level() != this.level();
    }

    private void tryRecoverLostTarget(ServerLevel sl) {
        if (this.attackState != 0 || this.isManualTestMode() || this.hasPersistentEpicActive()) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || target.level() != this.level()) {
            this.resetPursuitRecoveryState();
            return;
        }

        double distanceSqr = this.distanceToSqr(target);
        double verticalGap = Math.abs(target.getY() - this.getY());
        if (distanceSqr < 36.0D && verticalGap < 3.0D && this.targetNoSightTicks < 6) {
            this.pursuitStuckTicks = Math.max(0, this.pursuitStuckTicks - 4);
            return;
        }

        if (this.tickCount % 12 == 0) {
            this.samplePursuitStuckState(distanceSqr, verticalGap);
        }

        if (this.tryNaturalPursuitAssist(sl, target, distanceSqr, verticalGap)) {
            return;
        }

        if (this.pursuitRecoveryCooldown > 0) {
            return;
        }

        boolean severeLayerSplit = verticalGap > 6.0D && this.targetNoSightTicks > 10 && distanceSqr > 49.0D;
        boolean fullyStranded = distanceSqr > 256.0D && this.targetNoSightTicks > 18 && this.targetFarTicks > 10;
        if (!severeLayerSplit && !fullyStranded && this.pursuitStuckTicks < 24) {
            return;
        }

        Vec3 recoveryAnchor = this.findRecoveryAnchor(sl, target);
        if (recoveryAnchor == null) {
            return;
        }

        Vec3 origin = this.position().add(0.0D, 1.0D, 0.0D);
        AbyssalEffects.spawnInfectionCloud(sl, origin, 0.45D, 0.55D);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, origin.x, origin.y, origin.z, 8, 0.28D, 0.45D, 0.28D, 0.03D);

        this.teleportTo(recoveryAnchor.x, recoveryAnchor.y, recoveryAnchor.z);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
        this.hurtMarked = true;
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.6F, 0.64F);

        Vec3 arrival = recoveryAnchor.add(0.0D, 1.0D, 0.0D);
        AbyssalEffects.spawnInfectionCloud(sl, arrival, 0.65D, 0.55D);
        sl.sendParticles(ParticleTypes.SCULK_SOUL, arrival.x, arrival.y, arrival.z, 8, 0.25D, 0.35D, 0.25D, 0.01D);

        this.pursuitRecoveryCooldown = 90;
        this.pursuitStuckTicks = 0;
        this.lastPursuitSamplePos = this.position();
    }

    private boolean tryNaturalPursuitAssist(ServerLevel sl, LivingEntity target, double distanceSqr, double verticalGap) {
        if (this.pursuitAssistCooldown > 0 || !this.onGround() || distanceSqr < 16.0D || distanceSqr > 324.0D) {
            return false;
        }

        double deltaY = target.getY() - this.getY();
        boolean lostSight = this.targetNoSightTicks > 6;
        Vec3 toTarget = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return false;
        }

        Vec3 forward = horizontal.normalize();
        boolean upwardChase = deltaY > 2.75D && verticalGap < 10.0D && (lostSight || this.targetFarTicks > 6);
        boolean downwardChase = deltaY < -3.0D && verticalGap < 16.0D && (lostSight || this.pursuitStuckTicks > 8);
        if (!upwardChase && !downwardChase) {
            return false;
        }

        double horizontalSpeed = upwardChase ? 0.95D : 1.20D;
        double verticalSpeed = upwardChase ? 0.72D : 0.16D;
        Vec3 assistVelocity = forward.scale(horizontalSpeed).add(0.0D, verticalSpeed, 0.0D);

        this.getNavigation().stop();
        this.setDeltaMovement(assistVelocity);
        this.hasImpulse = true;
        this.hurtMarked = true;
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.playSound(SoundEvents.PHANTOM_SWOOP, 1.3F, upwardChase ? 0.78F : 0.92F);

        Vec3 effectPos = this.position().add(0.0D, 0.8D, 0.0D);
        AbyssalEffects.spawnFearBurst(sl, effectPos, 0.35D, 0.25D);
        AbyssalEffects.spawnInfectionCloud(sl, effectPos, 0.28D, 0.30D);

        this.pursuitAssistCooldown = upwardChase ? 20 : 16;
        this.lastPursuitSamplePos = this.position();
        return true;
    }

    private void samplePursuitStuckState(double distanceSqr, double verticalGap) {
        Vec3 samplePos = this.position();
        if (this.lastPursuitSamplePos == null) {
            this.lastPursuitSamplePos = samplePos;
            return;
        }

        double dx = samplePos.x - this.lastPursuitSamplePos.x;
        double dz = samplePos.z - this.lastPursuitSamplePos.z;
        double movedHorizontalSqr = dx * dx + dz * dz;
        boolean trouble = distanceSqr > 64.0D
                && (this.targetNoSightTicks > 10 || verticalGap > 4.0D || this.targetFarTicks > 10);

        if (trouble && movedHorizontalSqr < 0.49D) {
            this.pursuitStuckTicks = Math.min(40, this.pursuitStuckTicks + 6);
        } else {
            this.pursuitStuckTicks = Math.max(0, this.pursuitStuckTicks - 4);
        }

        this.lastPursuitSamplePos = samplePos;
    }

    private Vec3 findRecoveryAnchor(ServerLevel sl, LivingEntity target) {
        Vec3 away = this.position().subtract(target.position());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            Vec3 targetLook = target.getLookAngle().multiply(-1.0D, 0.0D, -1.0D);
            horizontal = new Vec3(targetLook.x, 0.0D, targetLook.z);
        }
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }

        double baseAngle = Math.atan2(horizontal.z, horizontal.x);
        double[] radii = {6.5D, 7.5D, 4.5D};
        double[] angleOffsets = {0.0D, Math.PI / 4.0D, -Math.PI / 4.0D, Math.PI / 2.5D, -Math.PI / 2.5D, Math.PI};

        for (double radius : radii) {
            for (double angleOffset : angleOffsets) {
                double angle = baseAngle + angleOffset;
                double x = target.getX() + Math.cos(angle) * radius;
                double z = target.getZ() + Math.sin(angle) * radius;
                Vec3 candidate = SkillCastHelper.findStandablePosition(this, sl, x, target.getY(), z, 8);
                if (candidate != null && candidate.distanceToSqr(target.position()) >= 12.25D) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private void resetPursuitRecoveryState() {
        this.pursuitAssistCooldown = 0;
        this.pursuitRecoveryCooldown = 0;
        this.pursuitStuckTicks = 0;
        this.lastPursuitSamplePos = null;
    }

    private void tickEntrance(ServerLevel sl) {
        int totalTicks = Math.max(1, AbyssalConfig.COMMON.entranceInvulnerableTicks.get());
        int elapsed = Math.max(0, totalTicks - this.entranceTicks);
        Vec3 center = this.position().add(0.0D, 1.1D, 0.0D);

        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
        this.setInvulnerable(true);

        if (elapsed == 0) {
            this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 2.5F, 0.55F);
            this.playSound(SoundEvents.WARDEN_HEARTBEAT, 2.4F, 0.62F);
        }
        if (elapsed == totalTicks / 2) {
            this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2.1F, 0.58F);
        }

        if (AbyssalConfig.shouldEmitDetailParticle(sl.getGameTime(), 2)) {
            double radius = 1.2D + elapsed * 0.035D;
            AbyssalEffects.spawnInfectionCloud(sl, center, radius, 0.45D);
            AbyssalEffects.spawnCharge(sl, center.add(0.0D, 0.7D, 0.0D), 0.35D + radius * 0.2D, 0.55D);
        }

        this.entranceTicks--;
        if (this.entranceTicks <= 0) {
            this.entranceTicks = 0;
            this.setInvulnerable(false);
            this.playSound(SoundEvents.WARDEN_ROAR, 3.0F, 0.72F);
            this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3.2F, 0.58F);
            AbyssalEffects.spawnImpact(sl, center, 1.8D, 0.85D);
        }
    }

    private void syncCombatData() {
        if (this.level().isClientSide()) {
            return;
        }
        this.entityData.set(DATA_ATTACK_STATE, this.attackState);
        this.entityData.set(DATA_ATTACK_TICK, this.attackTick);
        this.entityData.set(DATA_PHASE_TWO, this.isPhaseTwoActive());
        this.entityData.set(DATA_ENTRANCE_TICKS, this.entranceTicks);
    }

    private static int updateCombatCounter(int current, boolean active, int gain, int decay, int cap) {
        if (active) {
            return Math.min(cap, current + gain);
        }
        return Math.max(0, current - decay);
    }

    private void applyFollowUpFromFinishedAttack(int state, int variant) {
        switch (state) {
            case 2 -> {
                if (variant == 1) {
                    this.setFollowUpIntent(FOLLOW_UP_CLOSE, 20);
                } else {
                    this.setFollowUpIntent(FOLLOW_UP_RANGED, 24);
                }
            }
            case 3 -> {
                if (variant == 1) {
                    this.setFollowUpIntent(FOLLOW_UP_CLOSE, 28);
                } else if (variant == 2) {
                    this.setFollowUpIntent(FOLLOW_UP_RANGED, 26);
                } else {
                    this.setFollowUpIntent(FOLLOW_UP_EXECUTE, 24);
                }
            }
            case 4 -> {
                if (variant == 1) {
                    this.setFollowUpIntent(FOLLOW_UP_EXECUTE, 30);
                } else if (variant == 2) {
                    this.setFollowUpIntent(FOLLOW_UP_CHASE, 20);
                } else {
                    this.setFollowUpIntent(FOLLOW_UP_CHASE, 16);
                }
            }
            case 5 -> this.setFollowUpIntent(FOLLOW_UP_CLOSE, 24);
            case 11 -> this.setFollowUpIntent(FOLLOW_UP_RANGED, 22);
            default -> {
                if (this.followUpTicks < 6) {
                    this.setFollowUpIntent(FOLLOW_UP_NONE, 0);
                }
            }
        }
    }

    private void spawnAbyssalDeathEffect(ServerLevel sl) {
        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.58D, 0.0D);
        double width = this.getBbWidth() * 0.75D;

        if (this.deathTime == 1) {
            this.playSound(SoundEvents.WARDEN_HEARTBEAT, 2.8F, 0.58F);
            this.playSound(SoundEvents.ENDERMAN_SCREAM, 2.4F, 0.45F);
            sl.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z, 18, width, 0.65D, width, 0.01D);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 12, width, 0.8D, width, 0.04D);
        }

        if (this.deathTime <= 16 && this.deathTime % 4 == 0) {
            float pitch = 0.54F + this.deathTime * 0.02F;
            this.playSound(SoundEvents.WARDEN_HEARTBEAT, 1.8F, pitch);
        }

        if (this.deathTime == 10) {
            this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 2.6F, 0.72F);
        }

        if (this.deathTime == 18) {
            this.playSound(SoundEvents.SOUL_ESCAPE.value(), 2.7F, 0.72F);
            this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 3.0F, 0.82F);
        }

        if (this.deathTime % 2 == 0) {
            sl.sendParticles(ParticleTypes.SOUL, center.x, center.y - 0.55D, center.z, 6, width, 0.85D, width, 0.02D);
            sl.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y - 0.2D, center.z, 4, width * 0.8D, 0.7D, width * 0.8D, 0.01D);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 4, width, 0.75D, width, 0.01D);
        }

        if (this.deathTime % 3 == 0) {
            double radius = 0.45D + this.deathTime * 0.04D;
            for (int i = 0; i < 3; i++) {
                double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
                double x = center.x + Math.cos(angle) * radius;
                double y = center.y - 0.45D + this.getRandom().nextDouble() * this.getBbHeight();
                double z = center.z + Math.sin(angle) * radius;
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0.04D, 0.08D, 0.04D, 0.01D);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.02D, 0.03D, 0.02D, 0.005D);
            }
        }

        if (this.deathTime >= 16) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.1D, center.z, 14, width * 0.9D, 0.9D, width * 0.9D, 0.1D);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.15D, center.z, 8, 0.55D, 0.6D, 0.55D, 0.01D);
        }
    }
}
