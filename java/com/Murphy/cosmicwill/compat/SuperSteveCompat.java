package com.Murphy.cosmicwill.compat;

import com.Murphy.cosmicwill.nullification.ErasureContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SuperSteveCompat {

    private static final String PREFIX = "plz.lizi.supersteve.";

    private SuperSteveCompat() {
    }

    public static boolean isRoot(Entity entity) {
        return entity.getClass().getName().startsWith(PREFIX);
    }

    public static void beforeErase(Entity entity) {
        if (!isRoot(entity)) {
            return;
        }
        ReflectionUtil.invokeVoid(entity, new String[]{"ssSetHealth"}, 0.0F);
        removeInstanceEntries(entity);
    }

    public static List<Entity> collectPairedInstances(Entity entity) {
        List<Entity> result = new ArrayList<>();
        if (!isRoot(entity)) {
            return result;
        }

        Object instances = ReflectionUtil.getStaticFieldValue(
                "plz.lizi.supersteve.SuperSteveMod",
                "SS_INSTANCES"
        );
        if (!(instances instanceof Map<?, ?> map)) {
            return result;
        }

        try {
            Object instance = map.get(entity.getId());
            collectFromInstance(instance, entity, result);
        } catch (Throwable ignored) {
        }

        try {
            for (Object value : new ArrayList<>(map.values())) {
                collectFromInstance(value, entity, result);
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    private static void collectFromInstance(Object instance, Entity root, List<Entity> output) {
        if (instance == null) {
            return;
        }
        Object client = ReflectionUtil.getFieldValue(instance, "clientInstance");
        Object server = ReflectionUtil.getFieldValue(instance, "serverInstance");
        if (client instanceof Entity entity && entity != root && !output.contains(entity)) {
            output.add(entity);
        }
        if (server instanceof Entity entity && entity != root && !output.contains(entity)) {
            output.add(entity);
        }
    }

    private static void removeInstanceEntries(Entity entity) {
        Object instances = ReflectionUtil.getStaticFieldValue(
                "plz.lizi.supersteve.SuperSteveMod",
                "SS_INSTANCES"
        );
        if (!(instances instanceof Map<?, ?> map)) {
            return;
        }

        try {
            map.remove(entity.getId());
        } catch (Throwable ignored) {
        }

        try {
            map.entrySet().removeIf(entry -> {
                Object value = entry.getValue();
                Object client = ReflectionUtil.getFieldValue(value, "clientInstance");
                Object server = ReflectionUtil.getFieldValue(value, "serverInstance");
                return client == entity || server == entity;
            });
        } catch (Throwable ignored) {
        }
    }

    public static boolean isAuxiliary(ErasureContext context, Entity candidate) {
        if (!context.rootClassName().startsWith(PREFIX)) {
            return false;
        }
        if (!candidate.getClass().getName().startsWith(PREFIX)) {
            return false;
        }
        return !(candidate instanceof LivingEntity)
                && candidate.position().distanceToSqr(context.origin()) <= 256.0D * 256.0D;
    }

    public static void lastResort(Entity entity) {
        if (!isRoot(entity)) {
            return;
        }
        ReflectionUtil.invokeStaticVoid(
                "plz.lizi.supersteve.api.SSUtil",
                new String[]{"killEntity"},
                entity,
                true
        );
    }
}
