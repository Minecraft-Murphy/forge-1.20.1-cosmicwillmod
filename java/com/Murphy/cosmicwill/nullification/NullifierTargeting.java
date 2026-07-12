package com.Murphy.cosmicwill.nullification;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class NullifierTargeting {

    private static final double DESTRUCTION_HALF_ANGLE_COS =
            Math.cos(Math.toRadians(50.0D));

    private static final String LAST_SWEEP_TICK =
            "cosmicwill:last_destruction_sweep_tick";

    private NullifierTargeting() {
    }

    public static int performDestructionSweep(
            ServerPlayer player,
            @Nullable Entity anchor
    ) {
        long gameTime = player.level().getGameTime();
        if (player.getPersistentData().contains(LAST_SWEEP_TICK)
                && player.getPersistentData().getLong(LAST_SWEEP_TICK) == gameTime) {
            return 0;
        }
        player.getPersistentData().putLong(LAST_SWEEP_TICK, gameTime);

        Entity resolvedAnchor = resolveParent(anchor);
        double reach = naturalEntityReach(player);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();

        AABB searchBox = player.getBoundingBox()
                .inflate(reach + 1.5D, reach + 1.5D, reach + 1.5D);

        Predicate<Entity> predicate = entity -> entity != player
                && !entity.isRemoved()
                && !entity.isSpectator()
                && (entity instanceof LivingEntity || entity instanceof PartEntity<?>);

        List<Entity> candidates = player.level().getEntities(
                player,
                searchBox,
                predicate
        );

        Map<UUID, Entity> uniqueTargets = new LinkedHashMap<>();
        if (resolvedAnchor != null
                && resolvedAnchor != player
                && !resolvedAnchor.isRemoved()
                && withinReach(eye, resolvedAnchor, reach)) {
            uniqueTargets.put(resolvedAnchor.getUUID(), resolvedAnchor);
        }

        for (Entity candidate : candidates) {
            Entity target = resolveParent(candidate);
            if (target == null || target == player || target.isRemoved()) {
                continue;
            }
            if (!withinReach(eye, target, reach)) {
                continue;
            }

            boolean isAnchor = resolvedAnchor != null
                    && resolvedAnchor.getUUID().equals(target.getUUID());
            if (!isAnchor && !player.hasLineOfSight(target)) {
                continue;
            }

            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(eye);
            if (toTarget.lengthSqr() < 1.0E-8D
                    || isAnchor
                    || look.dot(toTarget.normalize()) >= DESTRUCTION_HALF_ANGLE_COS) {
                uniqueTargets.put(target.getUUID(), target);
            }
        }

        for (Entity target : uniqueTargets.values()) {
            NullificationManager.destructionSweep(target, player);
        }
        return uniqueTargets.size();
    }

    @Nullable
    public static Entity findEntityInSight(
            ServerPlayer player,
            double maxRange
    ) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 fullEnd = eye.add(look.scale(maxRange));

        BlockHitResult blockHit = player.level().clip(new ClipContext(
                eye,
                fullEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 rayEnd = blockHit.getType() == HitResult.Type.MISS
                ? fullEnd
                : blockHit.getLocation();

        AABB searchBox = player.getBoundingBox()
                .expandTowards(rayEnd.subtract(eye))
                .inflate(2.0D);

        Predicate<Entity> predicate = entity -> entity != player
                && !entity.isSpectator()
                && entity.isPickable()
                && !entity.isRemoved();

        List<Entity> candidates = player.level().getEntities(
                player,
                searchBox,
                predicate
        );

        Entity nearest = null;
        double nearestDistanceSqr = eye.distanceToSqr(rayEnd);

        for (Entity candidate : candidates) {
            AABB hitBox = candidate.getBoundingBox()
                    .inflate(candidate.getPickRadius() + 0.3D);

            if (hitBox.contains(eye)) {
                nearest = candidate;
                nearestDistanceSqr = 0.0D;
                continue;
            }

            Optional<Vec3> hit = hitBox.clip(eye, rayEnd);
            if (hit.isEmpty()) {
                continue;
            }

            double distanceSqr = eye.distanceToSqr(hit.get());
            if (distanceSqr < nearestDistanceSqr) {
                nearest = candidate;
                nearestDistanceSqr = distanceSqr;
            }
        }

        return resolveParent(nearest);
    }


    private static double naturalEntityReach(ServerPlayer player) {
        return player.isCreative() ? 5.0D : 3.0D;
    }

    private static boolean withinReach(Vec3 point, Entity entity, double reach) {
        AABB box = entity.getBoundingBox();
        double x = Math.max(box.minX, Math.min(point.x, box.maxX));
        double y = Math.max(box.minY, Math.min(point.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(point.z, box.maxZ));
        return point.distanceToSqr(x, y, z) <= reach * reach;
    }

    @Nullable
    public static Entity resolveParent(@Nullable Entity entity) {
        Entity current = entity;
        int guard = 0;
        while (current instanceof PartEntity<?> part && guard++ < 8) {
            Entity parent = part.getParent();
            if (parent == null || parent == current) {
                break;
            }
            current = parent;
        }
        return current;
    }
}
