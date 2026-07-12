package com.Murphy.cosmicwill.nullification;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public final class ErasureContext {

    public static final int RETRY_LIFETIME_TICKS = 40;

    private final UUID rootUuid;
    private final int rootEntityId;
    private final String rootClassName;
    private final String entityNamespace;
    private final ResourceKey<Level> dimension;
    private final Vec3 origin;
    private final ErasureMode mode;
    private final WeakReference<Entity> rootReference;
    private final Set<UUID> linkedUuids = ConcurrentHashMap.newKeySet();
    private final Queue<WeakReference<Entity>> pendingEntities = new ConcurrentLinkedQueue<>();

    private int age;

    public ErasureContext(Entity root, ErasureMode mode) {
        this.rootUuid = root.getUUID();
        this.rootEntityId = root.getId();
        this.rootClassName = root.getClass().getName();
        var registryName = ForgeRegistries.ENTITY_TYPES.getKey(root.getType());
        this.entityNamespace = registryName == null ? "" : registryName.getNamespace();
        this.dimension = root.level().dimension();
        this.origin = root.position();
        this.mode = mode;
        this.rootReference = new WeakReference<>(root);
        this.linkedUuids.add(rootUuid);
    }

    public UUID rootUuid() {
        return rootUuid;
    }

    public int rootEntityId() {
        return rootEntityId;
    }

    public String rootClassName() {
        return rootClassName;
    }

    public String entityNamespace() {
        return entityNamespace;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public Vec3 origin() {
        return origin;
    }

    public ErasureMode mode() {
        return mode;
    }

    @Nullable
    public Entity rootReference() {
        return rootReference.get();
    }

    public int age() {
        return age;
    }

    public int incrementAge() {
        return ++age;
    }

    public boolean isExpired() {
        return age > RETRY_LIFETIME_TICKS;
    }

    public boolean delayElapsed() {
        return age >= mode.eraseDelayTicks();
    }

    public void addLinked(Entity entity) {
        UUID uuid = entity.getUUID();
        if (linkedUuids.add(uuid)) {
            pendingEntities.add(new WeakReference<>(entity));
        }
        ErasureLedger.markErased(uuid);
    }

    public boolean containsUuid(UUID uuid) {
        return linkedUuids.contains(uuid);
    }

    @Nullable
    public Entity pollPending() {
        WeakReference<Entity> reference;
        while ((reference = pendingEntities.poll()) != null) {
            Entity entity = reference.get();
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    public boolean matchesReplacement(Entity entity) {
        return dimension.equals(entity.level().dimension())
                && containsUuid(entity.getUUID());
    }
}
