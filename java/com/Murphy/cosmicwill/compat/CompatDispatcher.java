package com.Murphy.cosmicwill.compat;

import com.Murphy.cosmicwill.nullification.ErasureContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CompatDispatcher {

    private CompatDispatcher() {
    }

    /**
     * 根实体真正从世界注册表移除之前调用。
     *
     * 与 beforeErase 分开，是为了让毁灭模式先完成一次正常奖励结算，
     * 再处理需要正式关闭战斗管理器的特殊 Boss。
     */
    public static void beforeRootErase(
            ErasureContext context,
            Entity entity
    ) {
        DraconicGuardianCompat.beforeRootErase(
                context,
                entity
        );
    }

    public static void beforeErase(ErasureContext context, Entity entity) {
        GenericBossBarCompat.closeBossBars(entity);
        WitherzillaCompat.beforeErase(entity);
        JzyyCompat.beforeErase(entity);
        OniMikoCompat.beforeErase(entity);
        SuperSteveCompat.beforeErase(entity);
    }

    public static List<Entity> collectPairedEntities(Entity root) {
        List<Entity> linked = new ArrayList<>();
        linked.addAll(SuperSteveCompat.collectPairedInstances(root));
        return linked;
    }

    public static boolean isAuxiliary(ErasureContext context, Entity candidate) {
        if (candidate == null || candidate.getUUID().equals(context.rootUuid())) {
            return false;
        }

        if (candidate instanceof PartEntity<?> part) {
            Entity parent = part.getParent();
            if (parent != null && context.containsUuid(parent.getUUID())) {
                return true;
            }
        }

        if (candidate instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner != null && context.containsUuid(owner.getUUID())) {
                return true;
            }
        }

        if (candidate.getVehicle() != null
                && context.containsUuid(candidate.getVehicle().getUUID())) {
            return true;
        }

        if (referencesRoot(candidate, context)) {
            return true;
        }

        return WitherzillaCompat.isAuxiliary(context, candidate)
                || JzyyCompat.isAuxiliary(context, candidate)
                || OniMikoCompat.isAuxiliary(context, candidate)
                || SuperSteveCompat.isAuxiliary(context, candidate);
    }

    private static boolean referencesRoot(Entity candidate, ErasureContext context) {
        if (candidate.position().distanceToSqr(context.origin()) > 384.0D * 384.0D) {
            return false;
        }

        var candidateKey = ForgeRegistries.ENTITY_TYPES.getKey(candidate.getType());
        String candidateNamespace = candidateKey == null ? "" : candidateKey.getNamespace();
        if (!context.entityNamespace().isEmpty()
                && !candidateNamespace.equals(context.entityNamespace())) {
            // Direct owner/projectile checks above are allowed cross-mod; reflective
            // ownership is restricted to the same mod namespace.
            return false;
        }

        for (Field field : ReflectionUtil.allFields(candidate.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(candidate);
                if (value instanceof Entity entity
                        && context.containsUuid(entity.getUUID())) {
                    return true;
                }
                if (value instanceof UUID uuid && context.containsUuid(uuid)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /**
     * 由 EntityRegistryEraser 查询外部最终清理器应该延迟多久。
     *
     * 凋零斯拉可以在一 Tick 后使用自己的强制移除器；
     * 执行之龙等多部件实体继续使用更安全的默认延迟。
     */
    public static int lastResortDelayTicks(Entity entity) {
        return WitherzillaCompat.lastResortDelayTicks(
                entity
        );
    }

    public static void lastResort(Entity entity) {
        WitherzillaCompat.lastResort(entity);
        JzyyCompat.lastResort(entity);
        OniMikoCompat.lastResort(entity);
        SuperSteveCompat.lastResort(entity);
    }
}
