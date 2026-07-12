package com.Murphy.cosmicwill.singularity;

import com.Murphy.cosmicwill.damage.CWDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class SingularityCatastrophe {

    private static final int HARD_CLEAR_RADIUS = 18;
    private static final float EXPLOSION_POWER = 20.0F;

    private static final float BLACK_HOLE_DAMAGE =
            2_147_000_000.0F;

    private static final ThreadLocal<Boolean> ACTIVE =
            ThreadLocal.withInitial(() -> false);

    private SingularityCatastrophe() {
    }

    public static void trigger(
            ServerLevel level,
            BlockPos center
    ) {
        if (ACTIVE.get()) {
            return;
        }

        ACTIVE.set(true);

        try {

            dealBlackHoleDamage(
                    level,
                    center
            );

            /*
             * 爆炸负责声音、闪光和击退。
             * 方块破坏继续由下方的无硬度球形清除处理。
             */
            level.explode(
                    null,
                    center.getX() + 0.5D,
                    center.getY() + 0.5D,
                    center.getZ() + 0.5D,
                    EXPLOSION_POWER,
                    true,
                    Level.ExplosionInteraction.NONE
            );

            level.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    center.getX() + 0.5D,
                    center.getY() + 0.5D,
                    center.getZ() + 0.5D,
                    24,
                    8.0D,
                    8.0D,
                    8.0D,
                    0.1D
            );

            clearSphereIgnoringHardness(
                    level,
                    center
            );
        } finally {
            ACTIVE.set(false);
        }
    }

    private static void dealBlackHoleDamage(
            ServerLevel level,
            BlockPos center
    ) {
        AABB area = new AABB(center)
                .inflate(
                        HARD_CLEAR_RADIUS + 1.0D
                );

        double centerX = center.getX() + 0.5D;
        double centerY = center.getY() + 0.5D;
        double centerZ = center.getZ() + 0.5D;

        double radiusSquared =
                (HARD_CLEAR_RADIUS + 1.0D)
                        * (HARD_CLEAR_RADIUS + 1.0D);

        for (LivingEntity living
                : level.getEntitiesOfClass(
                        LivingEntity.class,
                        area
                )) {
            if (living.distanceToSqr(
                    centerX,
                    centerY,
                    centerZ
            ) > radiusSquared) {
                continue;
            }

            living.invulnerableTime = 0;

            living.hurt(
                    CWDamageTypes.blackHole(level),
                    BLACK_HOLE_DAMAGE
            );
        }
    }

    private static void clearSphereIgnoringHardness(
            ServerLevel level,
            BlockPos center
    ) {
        int radiusSquared =
                HARD_CLEAR_RADIUS
                        * HARD_CLEAR_RADIUS;

        BlockPos.MutableBlockPos target =
                new BlockPos.MutableBlockPos();

        for (int dx = -HARD_CLEAR_RADIUS;
             dx <= HARD_CLEAR_RADIUS;
             dx++) {
            for (int dy = -HARD_CLEAR_RADIUS;
                 dy <= HARD_CLEAR_RADIUS;
                 dy++) {
                for (int dz = -HARD_CLEAR_RADIUS;
                     dz <= HARD_CLEAR_RADIUS;
                     dz++) {
                    if (dx * dx
                            + dy * dy
                            + dz * dz
                            > radiusSquared) {
                        continue;
                    }

                    target.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (!level.isInWorldBounds(target)
                            || level.getBlockState(target)
                            .isAir()) {
                        continue;
                    }

                    level.removeBlockEntity(target);

                    level.setBlock(
                            target,
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS
                                    | Block.UPDATE_SUPPRESS_DROPS
                    );
                }
            }
        }
    }
}
