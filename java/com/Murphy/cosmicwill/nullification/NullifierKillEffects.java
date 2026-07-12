package com.Murphy.cosmicwill.nullification;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class NullifierKillEffects {

    private static final String LAST_FX_TICK_TAG =
            "CosmicWill_LastExecutionFxTick";

    private NullifierKillEffects() {
    }

    public static void spawnDestruction(Entity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }

        if (!markEffectThisTick(target, level)) {
            return;
        }

        AABB box = target.getBoundingBox();

        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerY = (box.minY + box.maxY) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;

        double width = Mth.clamp(box.getXsize(), 0.6D, 7.0D);
        double height = Mth.clamp(box.getYsize(), 0.6D, 9.0D);
        double depth = Mth.clamp(box.getZsize(), 0.6D, 7.0D);

        int endRodCount = Mth.clamp(
                18 + (int) (width * height * 4.0D),
                18,
                96
        );

        int poofCount = Mth.clamp(
                8 + (int) (width * height * 1.7D),
                8,
                42
        );

        // Bright white fragments across the entity volume.
        level.sendParticles(
                ParticleTypes.END_ROD,
                centerX,
                centerY,
                centerZ,
                endRodCount,
                width * 0.52D,
                height * 0.52D,
                depth * 0.52D,
                0.055D
        );

        // Pale disintegration haze.
        level.sendParticles(
                ParticleTypes.POOF,
                centerX,
                centerY,
                centerZ,
                poofCount,
                width * 0.42D,
                height * 0.42D,
                depth * 0.42D,
                0.025D
        );

        // A few sharp energy fractures.
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                centerX,
                centerY,
                centerZ,
                Mth.clamp(endRodCount / 5, 4, 18),
                width * 0.35D,
                height * 0.35D,
                depth * 0.35D,
                0.035D
        );
    }

    public static void spawnNullification(Entity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }

        if (!markEffectThisTick(target, level)) {
            return;
        }

        AABB box = target.getBoundingBox();
        RandomSource random = level.random;

        double width = Mth.clamp(box.getXsize(), 0.7D, 7.0D);
        double height = Mth.clamp(box.getYsize(), 0.8D, 10.0D);
        double depth = Mth.clamp(box.getZsize(), 0.7D, 7.0D);

        int columns = Mth.clamp(
                3 + (int) Math.ceil((width + depth) * 0.45D),
                3,
                7
        );

        for (int column = 0; column < columns; column++) {
            double x = Mth.lerp(
                    random.nextDouble(),
                    box.minX,
                    box.maxX
            );

            double z = Mth.lerp(
                    random.nextDouble(),
                    box.minZ,
                    box.maxZ
            );

            double baseY = box.minY
                    + Math.min(0.22D, height * 0.08D)
                    + random.nextDouble()
                    * Math.min(0.35D, height * 0.12D);

            level.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x,
                    baseY,
                    z,
                    1,
                    0.035D,
                    0.025D,
                    0.035D,
                    0.012D
            );

            level.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    x,
                    baseY + random.nextDouble() * height * 0.25D,
                    z,
                    2,
                    0.06D,
                    0.12D,
                    0.06D,
                    0.018D
            );

            level.sendParticles(
                    ParticleTypes.SMOKE,
                    x,
                    baseY
                            + height
                            * (0.16D + random.nextDouble() * 0.32D),
                    z,
                    2,
                    0.045D,
                    0.14D,
                    0.045D,
                    0.015D
            );
        }
    }

    private static boolean markEffectThisTick(
            Entity target,
            ServerLevel level
    ) {
        CompoundTag data = target.getPersistentData();
        long gameTime = level.getGameTime();

        if (data.getLong(LAST_FX_TICK_TAG) == gameTime) {
            return false;
        }

        data.putLong(LAST_FX_TICK_TAG, gameTime);
        return true;
    }
}