package com.eddy1.tidesourcer.entity.ai;

import com.eddy1.tidesourcer.entity.ai.module.SkillCastHelper;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SunkenTitanCombatGoal extends Goal {
    private static final int INVALID_SCORE = -1000;
    private static final int MIN_BASIC_SCORE = 18;
    private static final int MIN_EPIC_SCORE = 52;

    private final TideSourcerEntity boss;

    public SunkenTitanCombatGoal(TideSourcerEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.boss.getTarget() != null && this.boss.getTarget().isAlive();
    }

    @Override
    public void tick() {
        LivingEntity target = this.boss.getTarget();
        if (target == null) {
            return;
        }

        this.boss.updateCombatMemory(target);
        boolean epicLocked = this.boss.hasPersistentEpicActive();

        if (this.boss.echoDomainConfusionTick > 0) {
            this.boss.getNavigation().stop();
            return;
        }

        this.boss.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.boss.attackState != 0) {
            this.boss.getNavigation().stop();
            return;
        }

        if (this.boss.isManualTestMode()) {
            this.boss.getNavigation().stop();
            return;
        }

        if (this.boss.horizontalCollision && target.getY() > this.boss.getY() + 1.0D) {
            this.boss.getJumpControl().jump();
        }

        if (this.handlePhaseUnlocks(epicLocked)) {
            return;
        }

        CombatContext context = this.buildContext(target);
        this.moveToTarget(context);

        if (this.tryRetreatTaunt(context)) {
            return;
        }

        if (!epicLocked && this.boss.cdGlobalEpic <= 0) {
            SkillOption epic = this.chooseEpic(context);
            if (epic != null && epic.score() >= MIN_EPIC_SCORE) {
                this.boss.startEpicAttack(epic.state());
                return;
            }
        }

        SkillOption basic = this.chooseBasic(context);
        if (basic != null && basic.score() >= MIN_BASIC_SCORE) {
            this.startBasicAttack(basic);
        }
    }

    private boolean handlePhaseUnlocks(boolean epicLocked) {
        float hpRatio = this.boss.getHealth() / this.boss.getMaxHealth();

        if (hpRatio <= 0.60F && !this.boss.phase60) {
            this.boss.phase60 = true;
            this.boss.phase70 = true;
            this.boss.phase80 = true;
            this.boss.phase90 = true;
            this.boss.pendingPhaseEpic = 6;
            if (!epicLocked) {
                this.boss.attackState = 100;
                return true;
            }
        } else if (hpRatio <= 0.70F && !this.boss.phase70) {
            this.boss.phase70 = true;
            this.boss.phase80 = true;
            this.boss.phase90 = true;
            this.boss.pendingPhaseEpic = 9;
            if (!epicLocked) {
                this.boss.attackState = 100;
                return true;
            }
        } else if (hpRatio <= 0.80F && !this.boss.phase80) {
            this.boss.phase80 = true;
            this.boss.phase90 = true;
            this.boss.pendingPhaseEpic = 8;
            if (!epicLocked) {
                this.boss.attackState = 100;
                return true;
            }
        } else if (hpRatio <= 0.90F && !this.boss.phase90) {
            this.boss.phase90 = true;
            this.boss.pendingPhaseEpic = 10;
            if (!epicLocked) {
                this.boss.attackState = 100;
                return true;
            }
        }

        if (this.boss.pendingPhaseEpic != 0 && !epicLocked) {
            this.boss.attackState = 100;
            return true;
        }

        return false;
    }

    private CombatContext buildContext(LivingEntity target) {
        double distanceSqr = this.boss.distanceToSqr(target);
        int nearbyEntities = this.boss.level().getEntitiesOfClass(
                LivingEntity.class,
                this.boss.getBoundingBox().inflate(8.0D),
                entity -> entity != this.boss && entity.isAlive()
        ).size();
        return new CombatContext(
                target,
                Math.sqrt(distanceSqr),
                distanceSqr,
                Math.abs(target.getY() - this.boss.getY()),
                this.boss.hasLineOfSight(target),
                target instanceof net.minecraft.world.entity.player.Player,
                nearbyEntities
        );
    }

    private void moveToTarget(CombatContext context) {
        double speed = 1.05D;
        if (context.distanceSqr() > 400.0D) {
            speed = 1.32D;
        } else if (context.distanceSqr() > 144.0D) {
            speed = 1.2D;
        }

        if (this.boss.isPhaseTwoActive()) {
            speed += context.distanceSqr() > 64.0D ? 0.16D : 0.08D;
        }
        if (this.boss.followUpIntent == TideSourcerEntity.FOLLOW_UP_CLOSE || this.boss.followUpIntent == TideSourcerEntity.FOLLOW_UP_CHASE) {
            speed += 0.14D;
        }
        if (this.boss.targetNoSightTicks > 10) {
            speed += 0.08D;
        }
        if (context.distanceSqr() < 16.0D && this.boss.followUpIntent == TideSourcerEntity.FOLLOW_UP_RANGED) {
            speed = 0.95D;
        }

        this.boss.getNavigation().moveTo(context.target(), speed);
    }

    private boolean tryRetreatTaunt(CombatContext context) {
        if (!context.playerTarget() || this.boss.cdTaunt > 0) {
            return false;
        }
        if (!this.canStartGroundedCast(0.9D)) {
            return false;
        }
        if (context.distanceSqr() < 1600.0D) {
            return false;
        }
        if (this.boss.targetStraightRunTicks < 8 && this.boss.targetFarTicks < 14) {
            return false;
        }

        this.boss.startAttack(11, 1);
        this.boss.cdTaunt = this.boss.scaleCooldown(260);
        return true;
    }

    private SkillOption chooseEpic(CombatContext context) {
        SkillOption best = null;
        best = this.pickHigher(best, 6, 1, this.scoreDomain(context));
        best = this.pickHigher(best, 7, 1, this.scoreMirage(context));
        best = this.pickHigher(best, 8, 1, this.scoreArmory(context));
        best = this.pickHigher(best, 9, 1, this.scoreNova(context));
        best = this.pickHigher(best, 10, 1, this.scoreSingularity(context));
        return best;
    }

    private SkillOption chooseBasic(CombatContext context) {
        SkillOption best = null;
        best = this.pickHigher(best, 1, 1, this.scoreSlash(context));
        best = this.pickHigher(best, 1, 2, this.scoreCombo(context));
        best = this.pickHigher(best, 1, 3, this.scoreWave(context));
        best = this.pickHigher(best, 2, 1, this.scoreLeap(context));
        best = this.pickHigher(best, 2, 2, this.scoreEruption(context));
        best = this.pickHigher(best, 2, 3, this.scoreSkyfall(context));
        best = this.pickHigher(best, 3, 1, this.scoreVortex(context));
        best = this.pickHigher(best, 3, 2, this.scoreRepulsion(context));
        best = this.pickHigher(best, 3, 3, this.scorePrison(context));
        best = this.pickHigher(best, 4, 1, this.scoreHook(context));
        best = this.pickHigher(best, 4, 2, this.scoreBreath(context));
        best = this.pickHigher(best, 4, 3, this.scoreRay(context));
        best = this.pickHigher(best, 5, 1, this.scoreDash(context));
        return best;
    }

    private void startBasicAttack(SkillOption option) {
        this.boss.startAttack(option.state(), option.variant());

        switch (option.state()) {
            case 1 -> {
                if (option.variant() == 1) {
                    this.boss.cdSlash = this.boss.scaleCooldown(26);
                } else if (option.variant() == 2) {
                    this.boss.cdCombo = this.boss.scaleCooldown(44);
                } else {
                    this.boss.cdWave = this.boss.scaleCooldown(96);
                }
            }
            case 2 -> {
                if (option.variant() == 1) {
                    this.boss.cdLeap = this.boss.scaleCooldown(82);
                } else if (option.variant() == 2) {
                    this.boss.cdEruption = this.boss.scaleCooldown(96);
                } else {
                    this.boss.cdSkyfall = this.boss.scaleCooldown(180);
                }
            }
            case 3 -> {
                if (option.variant() == 1) {
                    this.boss.cdVortex = this.boss.scaleCooldown(128);
                } else if (option.variant() == 2) {
                    this.boss.cdRepulsion = this.boss.scaleCooldown(112);
                } else {
                    this.boss.cdPrison = this.boss.scaleCooldown(185);
                }
            }
            case 4 -> {
                if (option.variant() == 1) {
                    this.boss.cdHook = this.boss.scaleCooldown(136);
                } else if (option.variant() == 2) {
                    this.boss.cdBreath = this.boss.scaleCooldown(142);
                } else {
                    this.boss.cdRay = this.boss.scaleCooldown(220);
                }
            }
            case 5 -> this.boss.cdDash = this.boss.scaleCooldown(94);
            default -> {
            }
        }
    }

    private SkillOption pickHigher(SkillOption current, int state, int variant, int score) {
        if (score <= INVALID_SCORE / 2) {
            return current;
        }
        SkillOption candidate = new SkillOption(state, variant, score);
        if (current == null || candidate.score() > current.score()) {
            return candidate;
        }
        return current;
    }

    private int scoreSlash(CombatContext context) {
        if (this.boss.cdSlash > 0 || context.distance() > 5.0D) {
            return INVALID_SCORE;
        }

        int score = 58 - (int) (context.distance() * 8.0D);
        score += this.boss.targetCloseTicks / 4;
        score += this.boss.targetApproachTicks / 4;
        return this.adjustBasicScore(score, 1, 1);
    }

    private int scoreCombo(CombatContext context) {
        if (this.boss.cdCombo > 0 || context.distance() < 2.4D || context.distance() > 7.5D) {
            return INVALID_SCORE;
        }

        int score = 52 - (int) (Math.abs(context.distance() - 4.5D) * 8.0D);
        score += this.boss.targetCloseTicks / 3;
        score += this.boss.targetApproachTicks / 5;
        return this.adjustBasicScore(score, 1, 2);
    }

    private int scoreWave(CombatContext context) {
        if (this.boss.cdWave > 0 || !context.hasSight() || context.distance() < 8.0D || context.distance() > 24.0D) {
            return INVALID_SCORE;
        }

        int score = 44 - (int) (Math.abs(context.distance() - 15.0D) * 3.0D);
        score += this.boss.targetStraightRunTicks / 2;
        score += this.boss.targetFarTicks / 5;
        if (this.boss.targetCloseTicks > 8) {
            score -= 18;
        }
        return this.adjustBasicScore(score, 1, 3);
    }

    private int scoreLeap(CombatContext context) {
        if (this.boss.cdLeap > 0 || context.distance() < 5.0D || context.distance() > 18.0D) {
            return INVALID_SCORE;
        }

        int score = 46 - (int) (Math.abs(context.distance() - 10.0D) * 4.0D);
        score += this.boss.targetRetreatTicks / 2;
        score += this.boss.targetStrafeTicks / 4;
        if (context.verticalGap() > 1.4D) {
            score += 8;
        }
        return this.adjustBasicScore(score, 2, 1);
    }

    private int scoreEruption(CombatContext context) {
        if (this.boss.cdEruption > 0 || context.distance() > 15.0D) {
            return INVALID_SCORE;
        }

        int score = 40 - (int) (Math.abs(context.distance() - 8.0D) * 4.0D);
        score += this.boss.targetStrafeTicks / 3;
        score += this.boss.targetCloseTicks / 4;
        return this.adjustBasicScore(score, 2, 2);
    }

    private int scoreSkyfall(CombatContext context) {
        if (this.boss.cdSkyfall > 0 || context.distance() < 10.0D || context.distance() > 30.0D) {
            return INVALID_SCORE;
        }

        int score = 42 - (int) (Math.abs(context.distance() - 18.0D) * 3.0D);
        score += this.boss.targetNoSightTicks / 2;
        score += this.boss.targetStraightRunTicks / 3;
        score += this.boss.targetFarTicks / 4;
        return this.adjustBasicScore(score, 2, 3);
    }

    private int scoreVortex(CombatContext context) {
        if (this.boss.cdVortex > 0 || context.distance() < 5.0D || context.distance() > 14.0D) {
            return INVALID_SCORE;
        }

        int score = 40 - (int) (Math.abs(context.distance() - 9.0D) * 4.0D);
        score += this.boss.targetRetreatTicks / 2;
        if (this.boss.targetCloseTicks > 12) {
            score -= 10;
        }
        return this.adjustBasicScore(score, 3, 1);
    }

    private int scoreRepulsion(CombatContext context) {
        if (this.boss.cdRepulsion > 0 || context.distance() > 9.0D || !this.canStartGroundedCast(0.9D)) {
            return INVALID_SCORE;
        }

        int score = 56 - (int) (context.distance() * 6.0D);
        score += this.boss.targetCloseTicks / 2;
        if (context.nearbyEntities() >= 3) {
            score += 14;
        }
        return this.adjustBasicScore(score, 3, 2);
    }

    private int scorePrison(CombatContext context) {
        if (this.boss.cdPrison > 0 || !context.hasSight() || context.distance() < 6.0D || context.distance() > 18.0D) {
            return INVALID_SCORE;
        }

        int score = 38 - (int) (Math.abs(context.distance() - 11.0D) * 3.0D);
        score += this.boss.targetStrafeTicks / 2;
        score += this.boss.targetAirTicks / 3;
        return this.adjustBasicScore(score, 3, 3);
    }

    private int scoreHook(CombatContext context) {
        if (this.boss.cdHook > 0 || !context.hasSight() || context.distance() < 6.0D || context.distance() > 20.0D || !this.canStartGroundedCast(0.9D)) {
            return INVALID_SCORE;
        }

        int score = 48 - (int) (Math.abs(context.distance() - 12.0D) * 4.0D);
        score += this.boss.targetRetreatTicks / 2;
        score += this.boss.targetStraightRunTicks / 3;
        return this.adjustBasicScore(score, 4, 1);
    }

    private int scoreBreath(CombatContext context) {
        if (this.boss.cdBreath > 0 || !context.hasSight() || context.distance() < 4.0D || context.distance() > 16.0D || !this.canStartGroundedCast(0.9D)) {
            return INVALID_SCORE;
        }

        int score = 36 - (int) (Math.abs(context.distance() - 9.0D) * 4.0D);
        score += this.boss.targetApproachTicks / 4;
        score += this.boss.targetStraightRunTicks / 4;
        return this.adjustBasicScore(score, 4, 2);
    }

    private int scoreRay(CombatContext context) {
        if (this.boss.cdRay > 0 || !context.hasSight() || context.distance() < 12.0D || context.distance() > 32.0D || !this.canStartGroundedCast(0.9D)) {
            return INVALID_SCORE;
        }

        int score = 48 - (int) (Math.abs(context.distance() - 20.0D) * 3.0D);
        score += this.boss.targetStraightRunTicks / 2;
        score += this.boss.targetFarTicks / 3;
        if (this.boss.targetCloseTicks > 8) {
            score -= 24;
        }
        if (this.boss.targetStrafeTicks > 12) {
            score -= 6;
        }
        return this.adjustBasicScore(score, 4, 3);
    }

    private int scoreDash(CombatContext context) {
        if (this.boss.cdDash > 0) {
            return INVALID_SCORE;
        }

        int score;
        if (context.distance() > 18.0D) {
            score = 42 + this.boss.targetFarTicks / 3;
        } else if (context.distance() < 4.5D) {
            score = 30 + this.boss.targetCloseTicks / 2;
        } else if (this.boss.targetNoSightTicks > 10) {
            score = 28 + this.boss.targetNoSightTicks / 2;
        } else if (this.boss.targetStraightRunTicks > 10) {
            score = 26 + this.boss.targetStraightRunTicks / 3;
        } else {
            return INVALID_SCORE;
        }

        return this.adjustBasicScore(score, 5, 1);
    }

    private int scoreDomain(CombatContext context) {
        if (!this.boss.phase60 || this.boss.cdDomain > 0 || context.distance() > 18.0D || !this.canStartGroundedCast(1.1D)) {
            return INVALID_SCORE;
        }

        int score = 58 - (int) (context.distance() * 2.0D);
        score += this.boss.targetCloseTicks / 3;
        score += this.boss.targetStrafeTicks / 4;
        if (context.playerTarget()) {
            score += 4;
        }
        return this.adjustEpicScore(score, 6, 1);
    }

    private int scoreMirage(CombatContext context) {
        if (this.boss.cdMirage > 0 || context.distance() < 7.0D || context.distance() > 24.0D) {
            return INVALID_SCORE;
        }

        int score = 54 - (int) (Math.abs(context.distance() - 14.0D) * 3.0D);
        score += this.boss.targetCloseTicks / 4;
        score += this.boss.targetStrafeTicks / 3;
        return this.adjustEpicScore(score, 7, 1);
    }

    private int scoreArmory(CombatContext context) {
        if (!this.boss.phase80 || this.boss.cdArmory > 0 || context.distance() < 12.0D) {
            return INVALID_SCORE;
        }

        int score = 52 - (int) (Math.abs(context.distance() - 20.0D) * 3.0D);
        score += this.boss.targetFarTicks / 3;
        score += this.boss.targetStraightRunTicks / 4;
        if (!context.hasSight()) {
            score -= 12;
        }
        return this.adjustEpicScore(score, 8, 1);
    }

    private int scoreNova(CombatContext context) {
        if (!this.boss.phase70 || this.boss.cdNova > 0 || context.distance() > 22.0D || !this.canStartGroundedCast(1.1D)) {
            return INVALID_SCORE;
        }

        int score = 56 - (int) (Math.abs(context.distance() - 10.0D) * 4.0D);
        score += this.boss.targetCloseTicks / 2;
        return this.adjustEpicScore(score, 9, 1);
    }

    private int scoreSingularity(CombatContext context) {
        if (!this.boss.phase90 || this.boss.cdSingularity > 0 || context.distance() < 10.0D) {
            return INVALID_SCORE;
        }

        int score = 56 - (int) (Math.abs(context.distance() - 18.0D) * 3.0D);
        score += this.boss.targetNoSightTicks / 2;
        score += this.boss.targetFarTicks / 3;
        score += this.boss.targetStraightRunTicks / 4;
        return this.adjustEpicScore(score, 10, 1);
    }

    private int adjustBasicScore(int score, int state, int variant) {
        score += this.followUpBonus(state, variant);
        if (this.boss.isPhaseTwoActive()) {
            if (state == 5 || state == 2 || state == 3) {
                score += 8;
            } else if (state == 4) {
                score += 5;
            }
        }
        score -= this.repetitionPenalty(state, variant);
        return score;
    }

    private int adjustEpicScore(int score, int state, int variant) {
        score += this.followUpBonus(state, variant);
        if (this.boss.isPhaseTwoActive()) {
            score += switch (state) {
                case 6, 7, 10 -> 14;
                case 8, 9 -> 10;
                default -> 6;
            };
        }
        if (this.boss.lastAttackStateUsed == state && this.boss.lastAttackVariantUsed == variant) {
            score -= 18;
        }
        return score;
    }

    private int repetitionPenalty(int state, int variant) {
        int penalty = 0;
        if (this.boss.lastAttackStateUsed == state) {
            penalty += 12;
        }
        if (this.boss.lastAttackStateUsed == state && this.boss.lastAttackVariantUsed == variant) {
            penalty += 22 + this.boss.repeatedAttackCount * 12;
        }
        return penalty;
    }

    private int followUpBonus(int state, int variant) {
        if (this.boss.followUpTicks <= 0) {
            return 0;
        }

        return switch (this.boss.followUpIntent) {
            case TideSourcerEntity.FOLLOW_UP_CLOSE -> switch (state) {
                case 1 -> variant == 1 ? 18 : variant == 2 ? 22 : 0;
                case 2 -> variant == 1 ? 18 : variant == 2 ? 8 : 0;
                case 3 -> variant == 3 ? 10 : 0;
                case 4 -> variant == 1 ? 12 : 0;
                default -> 0;
            };
            case TideSourcerEntity.FOLLOW_UP_CHASE -> switch (state) {
                case 2 -> variant == 1 ? 16 : variant == 3 ? 12 : 0;
                case 4 -> variant == 1 ? 16 : variant == 3 ? 10 : 0;
                case 5 -> 12;
                default -> 0;
            };
            case TideSourcerEntity.FOLLOW_UP_RANGED -> switch (state) {
                case 1 -> variant == 3 ? 18 : 0;
                case 2 -> variant == 2 ? 12 : variant == 3 ? 16 : 0;
                case 4 -> variant == 2 ? 14 : variant == 3 ? 22 : 0;
                case 8 -> 8;
                case 10 -> 16;
                default -> 0;
            };
            case TideSourcerEntity.FOLLOW_UP_EXECUTE -> switch (state) {
                case 1 -> variant == 2 ? 18 : variant == 1 ? 10 : 0;
                case 2 -> variant == 2 ? 20 : 0;
                case 3 -> variant == 3 ? 18 : 0;
                case 4 -> variant == 2 ? 12 : 0;
                case 7 -> 10;
                case 9 -> 18;
                default -> 0;
            };
            default -> 0;
        };
    }

    private boolean canStartGroundedCast(double tolerance) {
        if (!(this.boss.level() instanceof ServerLevel sl)) {
            return true;
        }
        return SkillCastHelper.isNearGround(this.boss, sl, tolerance);
    }

    private record CombatContext(
            LivingEntity target,
            double distance,
            double distanceSqr,
            double verticalGap,
            boolean hasSight,
            boolean playerTarget,
            int nearbyEntities
    ) {
    }

    private record SkillOption(int state, int variant, int score) {
    }
}
