package com.Murphy.cosmicwill.nullification;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ErasureLedger {

    private static final long UUID_TTL_NANOS = TimeUnit.SECONDS.toNanos(30L);
    private static final Map<UUID, Long> ERASED_UUIDS = new ConcurrentHashMap<>();

    private ErasureLedger() {
    }

    public static void markErased(UUID uuid) {
        ERASED_UUIDS.put(uuid, System.nanoTime() + UUID_TTL_NANOS);
    }

    public static void markErased(Entity entity) {
        markErased(entity.getUUID());
    }

    public static boolean isErased(UUID uuid) {
        Long expires = ERASED_UUIDS.get(uuid);
        if (expires == null) {
            return false;
        }
        if (expires < System.nanoTime()) {
            ERASED_UUIDS.remove(uuid, expires);
            return false;
        }
        return true;
    }

    public static boolean isErased(Entity entity) {
        return isErased(entity.getUUID())
                || entity.getPersistentData().getBoolean(NullificationManager.TAG_ERASURE_ROOT)
                || entity.getPersistentData().getBoolean(NullificationManager.TAG_RULE_NULLIFIED)
                || entity.getPersistentData().getBoolean(NullificationManager.TAG_CONCEPT_ERASED);
    }

    public static void purgeExpired() {
        long now = System.nanoTime();
        ERASED_UUIDS.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
