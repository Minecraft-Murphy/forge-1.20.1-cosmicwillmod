package com.Murphy.cosmicwill.nullification;

import com.Murphy.cosmicwill.compat.CompatDispatcher;
import com.Murphy.cosmicwill.compat.ReflectionUtil;
import com.Murphy.cosmicwill.network.CWNetwork;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraftforge.entity.PartEntity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public final class EntityRegistryEraser {

    private static final String TAG_REGISTRY_ERASED = "cosmicwill:registry_erased";

    private EntityRegistryEraser() {
    }

    public static void erase(
            ServerLevel level,
            Entity target,
            ErasureContext context
    ) {
        if (target == null || target instanceof ServerPlayer) {
            return;
        }

        UUID uuid = target.getUUID();
        int entityId = target.getId();

        ErasureLedger.markErased(uuid);
        CompatDispatcher.beforeErase(context, target);
        prepareObjectState(target);
        sendRemovalPackets(level, target, context);


        try {
            target.discard();
        } catch (Throwable ignored) {
        }


        if (isStillRegistered(level, target, uuid)) {
            invokeLevelRemovalCallback(target);
        }


        removeFromTickList(level, target);
        removeFromNavigation(level, target);
        removeFromChunkTracking(level, target);
        shallowManagerCleanup(level, target, entityId, uuid);


        int lastResortDelay = CompatDispatcher.lastResortDelayTicks(target);
        if (isStillRegistered(level, target, uuid)
                && context.age() >= lastResortDelay
                && !hasParts(target)) {
            CompatDispatcher.lastResort(target);
            invokeLevelRemovalCallback(target);
            removeFromTickList(level, target);
            removeFromNavigation(level, target);
            removeFromChunkTracking(level, target);
            shallowManagerCleanup(level, target, entityId, uuid);
        }

        forceTerminalFields(target);
        target.getPersistentData().putBoolean(TAG_REGISTRY_ERASED, true);


        if (!isStillRegistered(level, target, uuid)) {
            try {
                target.invalidateCaps();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void prepareObjectState(Entity target) {
        try {
            target.ejectPassengers();
        } catch (Throwable ignored) {
        }
        try {
            target.stopRiding();
        } catch (Throwable ignored) {
        }
        try {
            target.setInvisible(true);
            target.setSilent(true);
            target.setInvulnerable(false);
        } catch (Throwable ignored) {
        }
        if (target instanceof LivingEntity living) {
            try {
                living.removeAllEffects();
            } catch (Throwable ignored) {
            }
            NullificationManager.forceVisualHealth(living, 0.0F);
        }
        ReflectionUtil.setBooleanField(target, false, "canUpdate");
    }

    private static void invokeLevelRemovalCallback(Entity target) {
        Object rawCallback = ReflectionUtil.getFieldValue(
                target,
                "levelCallback", "f_146801_"
        );

        if (!(rawCallback instanceof EntityInLevelCallback callback)
                || callback == EntityInLevelCallback.NULL) {
            return;
        }

        try {
            callback.onRemove(Entity.RemovalReason.DISCARDED);
        } catch (Throwable ignored) {
        }
    }

    private static void removeFromTickList(ServerLevel level, Entity target) {
        Object tickList = fieldValueByType(level, "EntityTickList");
        if (tickList == null) {
            return;
        }
        ReflectionUtil.invokeVoid(
                tickList,
                new String[]{"remove", "m_156912_", "m_156822_", "m_188355_"},
                target
        );
    }

    private static void removeFromNavigation(ServerLevel level, Entity target) {
        if (!(target instanceof Mob)) {
            return;
        }

        Object navigatingMobs = ReflectionUtil.getFieldValue(
                level,
                "navigatingMobs", "f_143246_"
        );
        if (navigatingMobs instanceof Collection<?> collection) {
            try {
                collection.remove(target);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void removeFromChunkTracking(ServerLevel level, Entity target) {
        try {
            Object chunkSource = level.getChunkSource();
            ReflectionUtil.invokeVoid(
                    chunkSource,
                    new String[]{"removeEntity", "m_8386_", "m_143230_"},
                    target
            );
        } catch (Throwable ignored) {
        }
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void shallowManagerCleanup(
            ServerLevel level,
            Entity target,
            int entityId,
            UUID uuid
    ) {
        Object manager = fieldValueByType(level, "PersistentEntitySectionManager");
        if (manager == null) {
            return;
        }

        for (Field field : ReflectionUtil.allFields(manager.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(manager);

                if (value instanceof Set set) {
                    set.remove(uuid);
                    set.remove(target);
                    continue;
                }

                if (value instanceof Collection collection) {
                    collection.remove(target);
                    continue;
                }

                if (value instanceof Map map) {
                    removeFromTopLevelMap(map, target, entityId, uuid);
                    continue;
                }

                String simpleName = field.getType().getSimpleName();
                if ("EntityLookup".equals(simpleName)) {
                    ReflectionUtil.invokeVoid(
                            value,
                            new String[]{"remove", "m_156822_", "m_188355_"},
                            target
                    );
                    removeFromDirectLookupMaps(value, target, entityId, uuid);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void removeFromDirectLookupMaps(
            Object lookup,
            Entity target,
            int entityId,
            UUID uuid
    ) {
        if (lookup == null) {
            return;
        }

        for (Field field : ReflectionUtil.allFields(lookup.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(lookup);
                if (value instanceof Map map) {
                    removeFromTopLevelMap(map, target, entityId, uuid);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void removeFromTopLevelMap(
            Map map,
            Entity target,
            int entityId,
            UUID uuid
    ) {
        try {
            Object byId = map.get(entityId);
            if (byId == target || isSameUuidEntity(byId, uuid)) {
                map.remove(entityId);
            }
        } catch (Throwable ignored) {
        }

        try {
            Object byUuid = map.get(uuid);
            if (byUuid == target || isSameUuidEntity(byUuid, uuid)) {
                map.remove(uuid);
            }
        } catch (Throwable ignored) {
        }

        try {
            map.entrySet().removeIf(raw -> {
                Map.Entry entry = (Map.Entry) raw;
                Object key = entry.getKey();
                Object value = entry.getValue();
                return key == target
                        || value == target
                        || uuid.equals(key)
                        || isSameUuidEntity(value, uuid);
            });
        } catch (Throwable ignored) {
        }
    }

    private static boolean isSameUuidEntity(Object value, UUID uuid) {
        return value instanceof Entity entity
                && uuid.equals(entity.getUUID());
    }

    private static void forceTerminalFields(Entity target) {
        ReflectionUtil.setFieldValue(
                target,
                Entity.RemovalReason.DISCARDED,
                "removalReason", "f_146795_"
        );
        ReflectionUtil.setBooleanField(target, false, "isAddedToWorld");
        ReflectionUtil.setBooleanField(target, false, "canUpdate");

        ReflectionUtil.setFieldValue(
                target,
                EntityInLevelCallback.NULL,
                "levelCallback", "f_146801_"
        );
    }

    public static boolean isRegistered(ServerLevel level, Entity target) {
        return isStillRegistered(level, target, target.getUUID());
    }

    private static boolean isStillRegistered(
            ServerLevel level,
            Entity target,
            UUID uuid
    ) {
        try {
            Entity byUuid = level.getEntity(uuid);
            return byUuid == target || byUuid != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasParts(Entity entity) {
        try {
            PartEntity<?>[] parts = entity.getParts();
            return parts != null && parts.length > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void sendRemovalPackets(
            ServerLevel level,
            Entity target,
            ErasureContext context
    ) {
        int[] ids = collectRemovalIds(target);
        ClientboundRemoveEntitiesPacket vanillaPacket =
                new ClientboundRemoveEntitiesPacket(ids);

        for (ServerPlayer player : level.players()) {
            try {
                player.connection.send(vanillaPacket);
            } catch (Throwable ignored) {
            }
        }

        CWNetwork.sendErasure(level, target, context);
    }


    private static int[] collectRemovalIds(Entity target) {
        List<Integer> ids = new ArrayList<>();
        ids.add(target.getId());

        try {
            PartEntity<?>[] parts = target.getParts();
            if (parts != null) {
                for (PartEntity<?> part : parts) {
                    if (part != null) {
                        ids.add(part.getId());
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    private static Object fieldValueByType(Object owner, String simpleTypeName) {
        Field field = ReflectionUtil.findFieldByTypeSimpleName(owner, simpleTypeName);
        if (field == null) {
            return null;
        }
        try {
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
