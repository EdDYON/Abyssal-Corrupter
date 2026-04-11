package com.eddy1.tidesourcer.block.entity;

import com.eddy1.tidesourcer.block.AbyssalSummoningAltarBlock;
import com.eddy1.tidesourcer.config.AbyssalConfig;
import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import com.eddy1.tidesourcer.init.BlockEntityInit;
import com.eddy1.tidesourcer.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class AbyssalSummoningAltarBlockEntity extends BlockEntity {
    private static final double BOSS_CHECK_RADIUS = 96.0D;
    private int summoningTicks;
    private int cooldownTicks;
    private UUID summonerId;

    public AbyssalSummoningAltarBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityInit.ABYSSAL_SUMMONING_ALTAR.get(), pos, blockState);
    }

    public boolean tryStartSummoning(Player player, ItemStack catalyst) {
        if (this.level == null) {
            return false;
        }
        if (this.level.isClientSide()) {
            return true;
        }

        ServerLevel serverLevel = (ServerLevel) this.level;
        if (this.summoningTicks > 0) {
            player.displayClientMessage(Component.translatable("message.abyssal_corrupter.altar.already_summoning"), true);
            return false;
        }
        if (this.cooldownTicks > 0) {
            player.displayClientMessage(Component.translatable("message.abyssal_corrupter.altar.cooldown"), true);
            return false;
        }
        if (hasNearbyBoss(serverLevel, this.worldPosition)) {
            player.displayClientMessage(Component.translatable("message.abyssal_corrupter.altar.boss_nearby"), true);
            return false;
        }
        if (AbyssalConfig.COMMON.altarConsumesCatalyst.get() && !player.getAbilities().instabuild) {
            if (!catalyst.is(Items.ECHO_SHARD)) {
                player.displayClientMessage(Component.translatable("message.abyssal_corrupter.altar.needs_catalyst"), true);
                return false;
            }
            catalyst.shrink(1);
        }

        this.summonerId = player.getUUID();
        this.summoningTicks = Math.max(20, AbyssalConfig.COMMON.altarSummonTicks.get());
        this.setActive(true);
        this.setChanged();
        AbyssalEffects.play(serverLevel, Vec3.atCenterOf(this.worldPosition), SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.8F, 0.65F);
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbyssalSummoningAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (altar.cooldownTicks > 0) {
            altar.cooldownTicks--;
            if (altar.cooldownTicks == 0) {
                altar.setChanged();
            }
        }

        if (altar.summoningTicks <= 0) {
            return;
        }

        int totalTicks = Math.max(20, AbyssalConfig.COMMON.altarSummonTicks.get());
        int elapsed = totalTicks - altar.summoningTicks;
        Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 0.25D, 0.0D);

        altar.renderSummoning(serverLevel, center, elapsed, totalTicks);
        altar.summoningTicks--;

        if (altar.summoningTicks <= 0) {
            altar.finishSummon(serverLevel);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.summoningTicks = tag.getInt("SummoningTicks");
        this.cooldownTicks = tag.getInt("CooldownTicks");
        if (tag.hasUUID("Summoner")) {
            this.summonerId = tag.getUUID("Summoner");
        } else {
            this.summonerId = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("SummoningTicks", this.summoningTicks);
        tag.putInt("CooldownTicks", this.cooldownTicks);
        if (this.summonerId != null) {
            tag.putUUID("Summoner", this.summonerId);
        }
    }

    private void renderSummoning(ServerLevel serverLevel, Vec3 center, int elapsed, int totalTicks) {
        float progress = elapsed / (float) Math.max(1, totalTicks);
        double radius = 1.25D + progress * 3.8D;

        if (elapsed == 20) {
            AbyssalEffects.play(serverLevel, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.8F, 0.58F);
        }
        if (elapsed == 60) {
            AbyssalEffects.play(serverLevel, center, SoundEvents.WARDEN_HEARTBEAT, 2.4F, 0.6F);
        }
        if (elapsed == Math.max(0, totalTicks - 20)) {
            AbyssalEffects.play(serverLevel, center, SoundEvents.ELDER_GUARDIAN_CURSE, 2.2F, 0.55F);
            applyEntranceDarkness(serverLevel);
        }

        if (AbyssalConfig.shouldEmitDetailParticle(serverLevel.getGameTime(), 2)) {
            AbyssalEffects.spawnInfectionCloud(serverLevel, center.add(0.0D, 0.45D, 0.0D), 0.75D + progress, 0.35D);
        }
        if (AbyssalConfig.shouldEmitDetailParticle(serverLevel.getGameTime(), 4)) {
            for (int i = 0; i < 14; i++) {
                double angle = (Math.PI * 2.0D * i / 14.0D) - progress * Math.PI * 3.0D;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                AbyssalEffects.send(serverLevel, ParticleTypes.SOUL_FIRE_FLAME, x, center.y + 0.03D, z, 1, 0.03D, 0.01D, 0.03D, 0.0D);
                if (i % 2 == 0) {
                    AbyssalEffects.send(serverLevel, ParticleTypes.SCULK_SOUL, x, center.y + 0.08D, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }
        }
        if (elapsed % 10 == 0) {
            AbyssalEffects.send(serverLevel, ParticleTypes.REVERSE_PORTAL, center.add(0.0D, 1.0D + progress * 0.8D, 0.0D), 12, 0.8D, 0.5D, 0.8D, 0.06D);
        }
    }

    private void finishSummon(ServerLevel serverLevel) {
        Vec3 spawnPos = Vec3.atCenterOf(this.worldPosition).add(0.0D, 1.0D, 0.0D);
        TideSourcerEntity boss = EntityInit.ABYSSAL_CORRUPTER.get().create(serverLevel);
        if (boss != null) {
            boss.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, serverLevel.random.nextFloat() * 360.0F, 0.0F);
            boss.setPersistenceRequired();
            boss.beginEntrance(AbyssalConfig.COMMON.entranceInvulnerableTicks.get());

            Player summoner = this.summonerId == null ? null : serverLevel.getPlayerByUUID(this.summonerId);
            if (summoner != null && summoner.isAlive()) {
                boss.setTarget(summoner);
            }

            serverLevel.addFreshEntity(boss);
            AbyssalEffects.spawnImpact(serverLevel, spawnPos, 1.6D, 1.0D);
            AbyssalEffects.play(serverLevel, spawnPos, SoundEvents.WARDEN_ROAR, 3.0F, 0.72F);
        }

        this.summoningTicks = 0;
        this.cooldownTicks = Math.max(0, AbyssalConfig.COMMON.altarCooldownTicks.get());
        this.summonerId = null;
        this.setActive(false);
        this.setChanged();
    }

    private void applyEntranceDarkness(ServerLevel serverLevel) {
        int duration = 60;
        AABB area = new AABB(this.worldPosition).inflate(32.0D, 12.0D, 32.0D);
        List<Player> players = serverLevel.getEntitiesOfClass(Player.class, area, LivingEntity::isAlive);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0, false, false, false));
        }
    }

    private void setActive(boolean active) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.getBlockState();
        if (state.hasProperty(AbyssalSummoningAltarBlock.ACTIVE) && state.getValue(AbyssalSummoningAltarBlock.ACTIVE) != active) {
            this.level.setBlock(this.worldPosition, state.setValue(AbyssalSummoningAltarBlock.ACTIVE, active), 3);
        }
    }

    private static boolean hasNearbyBoss(ServerLevel serverLevel, BlockPos pos) {
        AABB area = new AABB(pos).inflate(BOSS_CHECK_RADIUS);
        return !serverLevel.getEntitiesOfClass(TideSourcerEntity.class, area, entity -> !entity.isClone && entity.isAlive()).isEmpty();
    }
}
