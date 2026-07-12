package com.Murphy.cosmicwill.nullification;

import com.Murphy.cosmicwill.mixins.LivingEntityInvoker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;

public final class RewardSettlement {

    private RewardSettlement() {
    }

    public static void settleOnce(
            ServerLevel level,
            LivingEntity target,
            ServerPlayer attacker
    ) {
        if (target.getPersistentData().getBoolean(
                NullificationManager.TAG_REWARD_SETTLED
        )) {
            return;
        }

        target.getPersistentData().putBoolean(
                NullificationManager.TAG_REWARD_SETTLED,
                true
        );
        target.getPersistentData().putBoolean(
                NullificationManager.TAG_REWARD_SETTLING,
                true
        );
        target.getPersistentData().putBoolean(
                NullificationManager.TAG_NO_DROPS,
                false
        );

        DamageSource source = target.damageSources().playerAttack(attacker);
        try {
            target.setLastHurtByPlayer(attacker);
            target.setLastHurtByMob(attacker);
            target.getCombatTracker().recordDamage(source, 1.0F);
        } catch (Throwable ignored) {
        }

        try {
            LivingEntityInvoker invoker = (LivingEntityInvoker) (Object) target;
            invoker.cosmicwill$invokeDropAllDeathLoot(source);
            invoker.cosmicwill$invokeDropExperience();
        } catch (Throwable ignored) {

        } finally {
            target.getPersistentData().putBoolean(
                    NullificationManager.TAG_REWARD_SETTLING,
                    false
            );
        }

        try {
            attacker.killedEntity(level, target);
            attacker.awardKillScore(target, 1, source);
        } catch (Throwable ignored) {
        }
        try {
            target.gameEvent(GameEvent.ENTITY_DIE);
        } catch (Throwable ignored) {
        }
    }
}
