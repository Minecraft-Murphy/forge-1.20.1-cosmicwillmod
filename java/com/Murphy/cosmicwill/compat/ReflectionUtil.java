package com.Murphy.cosmicwill.compat;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectionUtil {

    private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> MISSING_CLASSES = ConcurrentHashMap.newKeySet();

    private ReflectionUtil() {
    }

    @Nullable
    public static Class<?> findClass(String name) {
        Class<?> cached = CLASS_CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        if (MISSING_CLASSES.contains(name)) {
            return null;
        }

        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> type = Class.forName(name, false, contextLoader);
            CLASS_CACHE.put(name, type);
            return type;
        } catch (Throwable firstFailure) {
            try {
                Class<?> type = Class.forName(name);
                CLASS_CACHE.put(name, type);
                return type;
            } catch (Throwable ignored) {
                MISSING_CLASSES.add(name);
                return null;
            }
        }
    }

    public static boolean isClassPresent(String name) {
        return findClass(name) != null;
    }

    public static List<Field> allFields(Class<?> start) {
        List<Field> fields = new ArrayList<>();
        Class<?> cursor = start;
        while (cursor != null && cursor != Object.class) {
            Collections.addAll(fields, cursor.getDeclaredFields());
            cursor = cursor.getSuperclass();
        }
        return fields;
    }

    @Nullable
    public static Field findField(Class<?> start, String... names) {
        Class<?> cursor = start;
        while (cursor != null) {
            for (String name : names) {
                try {
                    Field field = cursor.getDeclaredField(name);
                    makeAccessible(field);
                    return field;
                } catch (Throwable ignored) {
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    @Nullable
    public static Field findFieldByTypeSimpleName(Object owner, String simpleName) {
        if (owner == null) {
            return null;
        }
        for (Field field : allFields(owner.getClass())) {
            try {
                if (field.getType().getSimpleName().equals(simpleName)) {
                    makeAccessible(field);
                    return field;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Nullable
    public static Object getFieldValue(@Nullable Object owner, String... names) {
        if (owner == null) {
            return null;
        }
        Field field = findField(owner.getClass(), names);
        if (field == null) {
            return null;
        }
        try {
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Object getStaticFieldValue(String className, String... names) {
        Class<?> type = findClass(className);
        if (type == null) {
            return null;
        }
        Field field = findField(type, names);
        if (field == null || !Modifier.isStatic(field.getModifiers())) {
            return null;
        }
        try {
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean setStaticFieldValue(
            String className,
            @Nullable Object value,
            String... names
    ) {
        Class<?> type = findClass(className);
        if (type == null) {
            return false;
        }
        Field field = findField(type, names);
        if (field == null || !Modifier.isStatic(field.getModifiers())) {
            return false;
        }
        try {
            field.set(null, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean setFieldValue(
            @Nullable Object owner,
            @Nullable Object value,
            String... names
    ) {
        if (owner == null) {
            return false;
        }
        Field field = findField(owner.getClass(), names);
        if (field == null) {
            return false;
        }
        try {
            field.set(owner, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean setBooleanField(
            @Nullable Object owner,
            boolean value,
            String... names
    ) {
        if (owner == null) {
            return false;
        }
        Field field = findField(owner.getClass(), names);
        if (field == null) {
            return false;
        }
        try {
            if (field.getType() == boolean.class) {
                field.setBoolean(owner, value);
            } else {
                field.set(owner, value);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean setNumberField(
            @Nullable Object owner,
            Number value,
            String... names
    ) {
        if (owner == null) {
            return false;
        }
        Field field = findField(owner.getClass(), names);
        if (field == null) {
            return false;
        }
        try {
            Class<?> type = field.getType();
            if (type == float.class || type == Float.class) {
                field.set(owner, value.floatValue());
            } else if (type == double.class || type == Double.class) {
                field.set(owner, value.doubleValue());
            } else if (type == int.class || type == Integer.class) {
                field.set(owner, value.intValue());
            } else if (type == long.class || type == Long.class) {
                field.set(owner, value.longValue());
            } else if (type == short.class || type == Short.class) {
                field.set(owner, value.shortValue());
            } else if (type == byte.class || type == Byte.class) {
                field.set(owner, value.byteValue());
            } else {
                return false;
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    public static Object invoke(@Nullable Object owner, String[] names, Object... args) {
        if (owner == null) {
            return null;
        }
        Method method = findCompatibleMethod(owner.getClass(), false, names, args);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(owner, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Object invokeStatic(String className, String[] names, Object... args) {
        Class<?> type = findClass(className);
        if (type == null) {
            return null;
        }
        Method method = findCompatibleMethod(type, true, names, args);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean invokeVoid(@Nullable Object owner, String[] names, Object... args) {
        if (owner == null) {
            return false;
        }
        Method method = findCompatibleMethod(owner.getClass(), false, names, args);
        if (method == null) {
            return false;
        }
        try {
            method.invoke(owner, args);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean invokeStaticVoid(String className, String[] names, Object... args) {
        Class<?> type = findClass(className);
        if (type == null) {
            return false;
        }
        Method method = findCompatibleMethod(type, true, names, args);
        if (method == null) {
            return false;
        }
        try {
            method.invoke(null, args);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static Method findCompatibleMethod(
            Class<?> start,
            boolean requireStatic,
            String[] names,
            Object[] args
    ) {
        Class<?> cursor = start;
        while (cursor != null) {
            for (Method method : cursor.getDeclaredMethods()) {
                if (requireStatic != Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!contains(names, method.getName())) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != args.length) {
                    continue;
                }
                boolean compatible = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!isCompatible(parameterTypes[i], args[i])) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) {
                    makeAccessible(method);
                    return method;
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    private static boolean contains(String[] names, String value) {
        for (String name : names) {
            if (name.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCompatible(Class<?> parameterType, @Nullable Object value) {
        if (value == null) {
            return !parameterType.isPrimitive();
        }
        return wrap(parameterType).isAssignableFrom(value.getClass());
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }

    public static void removeIdentityReferences(
            @Nullable Object root,
            @Nullable Object target,
            int entityId,
            UUID uuid,
            int maxDepth
    ) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        removeIdentityReferences(root, target, entityId, uuid, maxDepth, visited);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void removeIdentityReferences(
            @Nullable Object object,
            @Nullable Object target,
            int entityId,
            UUID uuid,
            int depth,
            Set<Object> visited
    ) {
        if (object == null || depth < 0 || visited.contains(object)) {
            return;
        }
        visited.add(object);

        if (object instanceof Map map) {
            removeMatchingMapEntries(map, target, entityId, uuid);
            if (depth > 0) {
                List<Object> values;
                try {
                    values = new ArrayList<>(map.values());
                } catch (Throwable ignored) {
                    values = List.of();
                }
                for (Object value : values) {
                    if (shouldRecurseInto(value)) {
                        removeIdentityReferences(
                                value, target, entityId, uuid, depth - 1, visited
                        );
                    }
                }
            }
            return;
        }

        if (object instanceof Collection collection) {
            try {
                collection.removeIf(value -> matchesTarget(value, target, entityId, uuid));
            } catch (Throwable ignored) {
                if (target != null) {
                    try {
                        collection.remove(target);
                    } catch (Throwable ignoredAgain) {
                    }
                }
                try {
                    collection.remove(uuid);
                } catch (Throwable ignoredAgain) {
                }
            }
            if (depth > 0) {
                List<Object> values;
                try {
                    values = new ArrayList<>(collection);
                } catch (Throwable ignored) {
                    values = List.of();
                }
                for (Object value : values) {
                    if (shouldRecurseInto(value)) {
                        removeIdentityReferences(
                                value, target, entityId, uuid, depth - 1, visited
                        );
                    }
                }
            }
            return;
        }

        Class<?> type = object.getClass();
        if (type.isArray()) {
            int length = Array.getLength(object);
            for (int index = 0; index < length; index++) {
                try {
                    Object value = Array.get(object, index);
                    if (matchesTarget(value, target, entityId, uuid)
                            && !type.getComponentType().isPrimitive()) {
                        Array.set(object, index, null);
                    } else if (depth > 0 && shouldRecurseInto(value)) {
                        removeIdentityReferences(
                                value, target, entityId, uuid, depth - 1, visited
                        );
                    }
                } catch (Throwable ignored) {
                }
            }
            return;
        }

        if (depth == 0 || !shouldInspectFields(type)) {
            return;
        }

        for (Field field : allFields(type)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                makeAccessible(field);
                Object value = field.get(object);
                if (matchesTarget(value, target, entityId, uuid)
                        && !field.getType().isPrimitive()) {
                    field.set(object, null);
                } else if (shouldRecurseInto(value)) {
                    removeIdentityReferences(
                            value, target, entityId, uuid, depth - 1, visited
                    );
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void removeMatchingMapEntries(
            Map map,
            @Nullable Object target,
            int entityId,
            UUID uuid
    ) {
        try {
            map.entrySet().removeIf(rawEntry -> {
                Map.Entry entry = (Map.Entry) rawEntry;
                Object key = entry.getKey();
                Object value = entry.getValue();
                return matchesTarget(key, target, entityId, uuid)
                        || matchesTarget(value, target, entityId, uuid)
                        || ((Integer.valueOf(entityId).equals(key) || uuid.equals(key))
                        && valueMatchesEntity(value, target, uuid));
            });
        } catch (Throwable ignored) {
        }
    }

    private static boolean valueMatchesEntity(
            @Nullable Object value,
            @Nullable Object target,
            UUID uuid
    ) {
        if (value == null) return false;
        if (target != null && value == target) return true;
        return value instanceof Entity entity && uuid.equals(entity.getUUID());
    }

    private static boolean matchesTarget(
            @Nullable Object value,
            @Nullable Object target,
            int entityId,
            UUID uuid
    ) {
        if (value == null) {
            return false;
        }
        if (target != null && value == target) {
            return true;
        }
        if (uuid.equals(value)) {
            return true;
        }
        if (value instanceof Entity entity) {
            return entity.getId() == entityId || uuid.equals(entity.getUUID());
        }
        return false;
    }

    public static boolean containsIdentityReference(
            @Nullable Object root,
            @Nullable Object target,
            int entityId,
            UUID uuid,
            int maxDepth
    ) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return containsIdentityReference(
                root, target, entityId, uuid, maxDepth, visited
        );
    }

    private static boolean containsIdentityReference(
            @Nullable Object object,
            @Nullable Object target,
            int entityId,
            UUID uuid,
            int depth,
            Set<Object> visited
    ) {
        if (object == null || depth < 0 || visited.contains(object)) {
            return false;
        }
        if (matchesTarget(object, target, entityId, uuid)) {
            return true;
        }
        visited.add(object);

        if (object instanceof Map<?, ?> map) {
            try {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (matchesTarget(entry.getKey(), target, entityId, uuid)
                            || matchesTarget(entry.getValue(), target, entityId, uuid)) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
            if (depth > 0) {
                try {
                    for (Object value : map.values()) {
                        if (shouldRecurseInto(value)
                                && containsIdentityReference(
                                value, target, entityId, uuid, depth - 1, visited
                        )) {
                            return true;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            return false;
        }

        if (object instanceof Collection<?> collection) {
            try {
                for (Object value : collection) {
                    if (matchesTarget(value, target, entityId, uuid)) {
                        return true;
                    }
                    if (depth > 0 && shouldRecurseInto(value)
                            && containsIdentityReference(
                            value, target, entityId, uuid, depth - 1, visited
                    )) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
            return false;
        }

        if (object.getClass().isArray()) {
            int length = Array.getLength(object);
            for (int index = 0; index < length; index++) {
                try {
                    Object value = Array.get(object, index);
                    if (matchesTarget(value, target, entityId, uuid)
                            || (depth > 0 && shouldRecurseInto(value)
                            && containsIdentityReference(
                            value, target, entityId, uuid, depth - 1, visited
                    ))) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            return false;
        }

        if (depth == 0 || !shouldInspectFields(object.getClass())) {
            return false;
        }
        for (Field field : allFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                makeAccessible(field);
                Object value = field.get(object);
                if (matchesTarget(value, target, entityId, uuid)) {
                    return true;
                }
                if (shouldRecurseInto(value)
                        && containsIdentityReference(
                        value, target, entityId, uuid, depth - 1, visited
                )) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static boolean shouldInspectFields(Class<?> type) {
        String name = type.getName();
        return name.startsWith("net.minecraft.world.level.entity.")
                || name.startsWith("net.minecraft.server.level.")
                || name.startsWith("net.minecraft.client.multiplayer.")
                || name.startsWith("net.minecraft.util.")
                || name.startsWith("it.unimi.dsi.fastutil.")
                || name.startsWith("com.wzz.witherzilla.")
                || name.startsWith("plz.lizi.supersteve.")
                || name.startsWith("com.csdy.jzyy.")
                || name.startsWith("oni_miko.");
    }

    private static boolean shouldRecurseInto(@Nullable Object value) {
        if (value == null) {
            return false;
        }
        return value instanceof Map
                || value instanceof Collection
                || value.getClass().isArray()
                || shouldInspectFields(value.getClass());
    }

    private static void makeAccessible(Field field) {
        try {
            field.setAccessible(true);
        } catch (Throwable ignored) {
        }
    }

    private static void makeAccessible(Method method) {
        try {
            method.setAccessible(true);
        } catch (Throwable ignored) {
        }
    }
}
