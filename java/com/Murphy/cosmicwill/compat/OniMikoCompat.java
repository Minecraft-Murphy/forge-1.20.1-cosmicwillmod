package com.Murphy.cosmicwill.compat;

import com.Murphy.cosmicwill.nullification.ErasureContext;
import com.Murphy.cosmicwill.nullification.ErasureLedger;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OniMikoCompat {

    private static final String ROOT = "oni_miko.entity.OnimikoEntity";
    private static final String PROJECTILE = "oni_miko.entity.T1Entity";

    private OniMikoCompat() {
    }

    public static boolean isRoot(Entity entity) {
        return ROOT.equals(entity.getClass().getName());
    }

    public static void beforeErase(Entity entity) {
        if (!isRoot(entity)) {
            return;
        }

        try {
            Object healthAccessor = ReflectionUtil.getStaticFieldValue(ROOT, "health");
            if (healthAccessor != null) {
                ReflectionUtil.invokeVoid(
                        entity.getEntityData(),
                        new String[]{"set", "m_135381_", "m_135372_"},
                        healthAccessor,
                        0.0F
                );
            }
        } catch (Throwable ignored) {
        }

        GenericBossBarCompat.closeBossBars(entity);
    }

    public static boolean isAuxiliary(ErasureContext context, Entity candidate) {
        if (!ROOT.equals(context.rootClassName())) {
            return false;
        }
        if (!PROJECTILE.equals(candidate.getClass().getName())) {
            return false;
        }
        return candidate.position().distanceToSqr(context.origin()) <= 320.0D * 320.0D;
    }

    /** Called by the optional pseudo mixin on oni_miko.event.TickLateEvent. */
    public static boolean shouldCancelTickLateEvent(Object tickLateEvent) {
        try {
            Object task = ReflectionUtil.getFieldValue(tickLateEvent, "task");
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            return containsErasedReference(task, 0, visited);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean containsErasedReference(
            Object object,
            int depth,
            Set<Object> visited
    ) {
        if (object == null || depth > 6 || visited.contains(object)) {
            return false;
        }
        visited.add(object);

        if (object instanceof Entity entity) {
            return ErasureLedger.isErased(entity);
        }
        if (object instanceof UUID uuid) {
            return ErasureLedger.isErased(uuid);
        }
        if (object instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                if (containsErasedReference(key, depth + 1, visited)) {
                    return true;
                }
            }
            for (Object value : map.values()) {
                if (containsErasedReference(value, depth + 1, visited)) {
                    return true;
                }
            }
            return false;
        }
        if (object instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (containsErasedReference(value, depth + 1, visited)) {
                    return true;
                }
            }
            return false;
        }
        if (object.getClass().isArray()) {
            int length = Array.getLength(object);
            for (int index = 0; index < length; index++) {
                if (containsErasedReference(Array.get(object, index), depth + 1, visited)) {
                    return true;
                }
            }
            return false;
        }

        String className = object.getClass().getName();
        if (!(className.startsWith("oni_miko.")
                || className.contains("$$Lambda$")
                || className.contains("$Lambda/"))) {
            return false;
        }

        for (Field field : ReflectionUtil.allFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(object);
                if (containsErasedReference(value, depth + 1, visited)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static void resetClientResidue() {
        String clientEvent = "oni_miko.client.ClientEvent";
        ReflectionUtil.setStaticFieldValue(clientEvent, false, "cantMove");
        ReflectionUtil.setStaticFieldValue(clientEvent, 0, "cantMoveTicks");
        ReflectionUtil.setStaticFieldValue(clientEvent, null, "cantMoveParticle");
        ReflectionUtil.setStaticFieldValue(clientEvent, false, "genRedParticle");
        ReflectionUtil.setStaticFieldValue(clientEvent, 0, "genTicks");

        String gui = "oni_miko.client.ingame.IngameGuiRender";
        ReflectionUtil.setStaticFieldValue(gui, false, "renderRedScreen");
        ReflectionUtil.setStaticFieldValue(gui, 0, "redScreenTime");
        ReflectionUtil.setStaticFieldValue(gui, false, "renderErrorScreen");
    }

    public static void lastResort(Entity entity) {
        if (!isRoot(entity)) {
            return;
        }
        ReflectionUtil.invokeStaticVoid(
                "oni_miko.misite.ForceAttack",
                new String[]{"removeEntity"},
                entity
        );
    }
}
