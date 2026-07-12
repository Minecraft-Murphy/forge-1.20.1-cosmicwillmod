package com.Murphy.cosmicwill.items;

import com.Murphy.cosmicwill.damage.CWDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public final class ChargedEnergyBladeItem
        extends SwordItem {

    private static final double EXPLOSION_RADIUS = 12.0D;
    private static final float MAX_EXPLOSION_DAMAGE = 1000.0F;

    public ChargedEnergyBladeItem(
            Tier tier,
            int attackDamageModifier,
            float attackSpeedModifier,
            Properties properties
    ) {
        super(
                tier,
                attackDamageModifier,
                attackSpeedModifier,
                properties
        );
    }

    @Override
    public boolean hurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker
    ) {
        boolean result =
                super.hurtEnemy(
                        stack,
                        target,
                        attacker
                );

        /*
         * 击中玩家时只有剑本身的 64 点面板伤害，
         * 不触发范围爆震。
         */
        if (target instanceof Player
                || !(attacker.level()
                instanceof ServerLevel level)) {
            return result;
        }

        triggerStellarBlast(
                level,
                target,
                attacker
        );

        return result;
    }

    private static void triggerStellarBlast(
            ServerLevel level,
            LivingEntity target,
            LivingEntity attacker
    ) {
        Vec3 center =
                target.position()
                        .add(
                                0.0D,
                                target.getBbHeight()
                                        * 0.5D,
                                0.0D
                        );

        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                4.0F,
                0.72F
                        + level.random.nextFloat()
                        * 0.16F
        );

        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                center.x,
                center.y,
                center.z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );

        level.sendParticles(
                ParticleTypes.EXPLOSION,
                center.x,
                center.y,
                center.z,
                42,
                3.2D,
                2.4D,
                3.2D,
                0.08D
        );

        level.sendParticles(
                ParticleTypes.FLAME,
                center.x,
                center.y,
                center.z,
                72,
                4.5D,
                3.0D,
                4.5D,
                0.12D
        );

        AABB area =
                new AABB(
                        center,
                        center
                ).inflate(
                        EXPLOSION_RADIUS
                );

        for (LivingEntity entity
                : level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        entity ->
                                entity.isAlive()
                                        && entity != attacker
                                        && !(entity
                                        instanceof Player)
                )) {
            Vec3 entityCenter =
                    entity.position()
                            .add(
                                    0.0D,
                                    entity.getBbHeight()
                                            * 0.5D,
                                    0.0D
                            );

            double distance =
                    entityCenter.distanceTo(
                            center
                    );

            if (distance > EXPLOSION_RADIUS) {
                continue;
            }

            double exposure =
                    Mth.clamp(
                            1.0D
                                    - distance
                                    / EXPLOSION_RADIUS,
                            0.0D,
                            1.0D
                    );

            float damage =
                    (float) (
                            MAX_EXPLOSION_DAMAGE
                                    * exposure
                    );

            if (damage < 1.0F) {
                continue;
            }

            /*
             * 目标刚被剑击中，需清除同一 Tick 的受伤无敌帧，
             * 但伤害本身仍正常经过护甲、韧性与爆炸保护。
             */
            entity.invulnerableTime = 0;

            entity.hurt(
                    CWDamageTypes
                            .chargedBladeExplosion(
                                    level,
                                    attacker
                            ),
                    damage
            );

            Vec3 away =
                    entityCenter.subtract(
                            center
                    );

            if (away.lengthSqr() < 0.0001D) {
                away = new Vec3(
                        0.0D,
                        1.0D,
                        0.0D
                );
            } else {
                away = away.normalize();
            }

            double knockback =
                    0.55D
                            + exposure
                            * 2.25D;

            entity.push(
                    away.x * knockback,
                    0.28D
                            + exposure
                            * 0.72D,
                    away.z * knockback
            );
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.charged_energy_blade.charged"
                ).withStyle(ChatFormatting.GOLD)
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.charged_energy_blade.final_material"
                ).withStyle(ChatFormatting.DARK_PURPLE)
        );

        super.appendHoverText(
                stack,
                level,
                tooltip,
                flag
        );
    }
}
