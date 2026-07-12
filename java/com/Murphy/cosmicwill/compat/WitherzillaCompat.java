package com.Murphy.cosmicwill.compat;

import com.Murphy.cosmicwill.nullification.ErasureContext;
import net.minecraft.world.entity.Entity;

import java.util.Collection;

public final class WitherzillaCompat {

    private static final String WITHERZILLA =
            "com.wzz.witherzilla.entity.WitherzillaEntity";
    private static final String EXECUTION_DRAGON =
            "com.wzz.witherzilla.entity.ExecutionDragonEntity";

    private WitherzillaCompat() {
    }

    public static boolean isRoot(Entity entity) {
        if (entity == null) {
            return false;
        }
        String name = entity.getClass().getName();
        return WITHERZILLA.equals(name) || EXECUTION_DRAGON.equals(name);
    }

    public static boolean isWitherzilla(Entity entity) {
        return entity != null && WITHERZILLA.equals(entity.getClass().getName());
    }

    public static boolean isExecutionDragon(Entity entity) {
        return entity != null && EXECUTION_DRAGON.equals(entity.getClass().getName());
    }

    public static void beforeErase(Entity entity) {
        if (!isRoot(entity)) {
            return;
        }

        ReflectionUtil.setBooleanField(entity, true, "isDead");
        removeFromPrivateLivingList(entity);
    }

    public static void beforeClientErase(Entity entity) {
        beforeErase(entity);
    }

    private static void removeFromPrivateLivingList(Entity entity) {
        Object livingEntities = ReflectionUtil.getStaticFieldValue(
                "com.wzz.witherzilla.event.EventHandle",
                "livingEntities"
        );
        if (livingEntities instanceof Collection<?> collection) {
            try {
                collection.remove(entity);
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean isAuxiliary(
            ErasureContext context,
            Entity candidate
    ) {
        if (!context.rootClassName().startsWith(
                "com.wzz.witherzilla.entity."
        )) {
            return false;
        }

        String name = candidate.getClass().getName();
        boolean knownSkill = name.equals(
                "com.wzz.witherzilla.entity.WitherzillaSkull"
        ) || name.equals(
                "com.wzz.witherzilla.entity.ExpandingLightningCircleEntity"
        ) || name.equals(
                "com.wzz.witherzilla.entity.PurpleLightingEntity"
        ) || name.equals(
                "com.wzz.witherzilla.entity.RainbowLightingEntity"
        );

        return knownSkill
                && candidate.position().distanceToSqr(context.origin())
                <= 512.0D * 512.0D;
    }

    public static int lastResortDelayTicks(Entity entity) {
        return isWitherzilla(entity) ? 1 : 6;
    }

    public static void lastResort(Entity entity) {
        if (!isWitherzilla(entity)) {
            return;
        }

        beforeErase(entity);
        ReflectionUtil.invokeStaticVoid(
                "com.wzz.witherzilla.util.ModUtil",
                new String[]{"forceRemoveEntity"},
                entity
        );
    }
}
