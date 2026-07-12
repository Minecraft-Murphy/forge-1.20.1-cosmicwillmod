package com.Murphy.cosmicwill.client;

import com.Murphy.cosmicwill.compat.ReflectionUtil;
import com.Murphy.cosmicwill.compat.WitherzillaCompat;
import com.Murphy.cosmicwill.nullification.ErasureLedger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.PartEntity;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class ClientRegistryEraser {

    private ClientRegistryEraser() {
    }

    public static void erase(
            int entityId,
            UUID uuid,
            String rootClassName
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        ErasureLedger.markErased(uuid);

        Entity root = null;
        try {
            root = level.getEntity(entityId);
        } catch (Throwable ignored) {
        }

        if (root != null) {

            WitherzillaCompat.beforeClientErase(root);

            ReflectionUtil.setFieldValue(
                    root,
                    Entity.RemovalReason.DISCARDED,
                    "removalReason", "f_146795_"
            );
        }

        removeFromClientLevel(level, entityId);

        boolean rootStillPresent = false;
        try {
            rootStillPresent = root != null && level.getEntity(entityId) == root;
        } catch (Throwable ignored) {
        }

        if (rootStillPresent && root != null) {
            try {
                root.discard();
            } catch (Throwable ignored) {
            }

            try {
                rootStillPresent = level.getEntity(entityId) == root;
            } catch (Throwable ignored) {
            }
        }

        if (rootStillPresent && root != null) {
            forceTopLevelClientRemoval(level, root, entityId, uuid);
            try {
                rootStillPresent = level.getEntity(entityId) == root;
            } catch (Throwable ignored) {
                rootStillPresent = false;
            }
        }

        if (!rootStillPresent && root != null) {
            removePartsAfterParent(level, root);
        }
    }

    private static void removeFromClientLevel(
            ClientLevel level,
            int entityId
    ) {
        ReflectionUtil.invokeVoid(
                level,
                new String[]{"removeEntity", "m_171642_"},
                entityId,
                Entity.RemovalReason.DISCARDED
        );
    }

    private static void forceTopLevelClientRemoval(
            ClientLevel level,
            Entity root,
            int entityId,
            UUID uuid
    ) {
        Object ticking = ReflectionUtil.getFieldValue(
                level,
                "tickingEntities", "f_171630_"
        );
        ReflectionUtil.invokeVoid(
                ticking,
                new String[]{"remove", "m_156912_", "m_156822_"},
                root
        );

        Object partMap = ReflectionUtil.getFieldValue(level, "partEntities");
        removeDirectMapEntry(partMap, entityId, uuid, root);

        Object manager = null;
        var managerField = ReflectionUtil.findFieldByTypeSimpleName(
                level,
                "TransientEntitySectionManager"
        );
        if (managerField != null) {
            try {
                manager = managerField.get(level);
            } catch (Throwable ignored) {
            }
        }

        Object lookup = null;
        if (manager != null) {
            var lookupField = ReflectionUtil.findFieldByTypeSimpleName(
                    manager,
                    "EntityLookup"
            );
            if (lookupField != null) {
                try {
                    lookup = lookupField.get(manager);
                } catch (Throwable ignored) {
                }
            }
        }

        ReflectionUtil.invokeVoid(
                lookup,
                new String[]{"remove", "m_156822_", "m_188355_"},
                root
        );

        if (lookup != null) {
            for (var field : ReflectionUtil.allFields(lookup.getClass())) {
                try {
                    field.setAccessible(true);
                    removeDirectMapEntry(
                            field.get(lookup),
                            entityId,
                            uuid,
                            root
                    );
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void removeDirectMapEntry(
            Object object,
            int entityId,
            UUID uuid,
            Entity root
    ) {
        if (!(object instanceof java.util.Map map)) {
            return;
        }

        try {
            Object value = map.get(entityId);
            if (value == root) {
                map.remove(entityId);
            }
        } catch (Throwable ignored) {
        }

        try {
            Object value = map.get(uuid);
            if (value == root) {
                map.remove(uuid);
            }
        } catch (Throwable ignored) {
        }

        try {
            map.entrySet().removeIf(raw -> {
                java.util.Map.Entry entry = (java.util.Map.Entry) raw;
                return entry.getKey() == root
                        || entry.getValue() == root
                        || uuid.equals(entry.getKey());
            });
        } catch (Throwable ignored) {
        }
    }

    private static void removePartsAfterParent(
            ClientLevel level,
            Entity root
    ) {
        PartEntity<?>[] parts;
        try {
            parts = root.getParts();
        } catch (Throwable ignored) {
            return;
        }

        if (parts == null) {
            return;
        }

        for (PartEntity<?> part : parts) {
            if (part != null) {
                removeFromClientLevel(level, part.getId());
            }
        }
    }
}
