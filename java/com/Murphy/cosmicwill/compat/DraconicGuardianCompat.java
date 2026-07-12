package com.Murphy.cosmicwill.compat;

import com.Murphy.cosmicwill.nullification.ErasureContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 龙之研究混沌守卫
 *
 * 不把 Draconic Evolution 作为编译依赖。
 *
 * 关键不是继续增加伤害，而是完成它自己的战斗管理器生命周期：
 * DraconicGuardianEntity#kill 会调用
 * GuardianFightManager#processDragonDeath，后者才会把 guardianKilled
 * 写为 true、标记混沌水晶已被击败并注销战斗管理器。
 *
 * 单纯 discard 会跳过这条路径，管理器在 1200 Tick 后发现守卫消失，
 * 随后重新生成一只新的守卫。
 */
public final class DraconicGuardianCompat {

    private static final String GUARDIAN_CLASS =
            "com.brandon3055.draconicevolution.entity.guardian."
                    + "DraconicGuardianEntity";

    private static final String MANAGER_CLASS =
            "com.brandon3055.draconicevolution.entity.guardian."
                    + "GuardianFightManager";

    private static final String TAG_FINALIZED =
            "cosmicwill:draconic_guardian_fight_finalized";

    private static final String GUARDIAN_HEART_TAG =
            "guardian_heart";

    private DraconicGuardianCompat() {
    }

    public static void beforeRootErase(
            ErasureContext context,
            Entity entity
    ) {
        if (!isChaosGuardian(entity)
                || entity.getPersistentData()
                .getBoolean(TAG_FINALIZED)
                || !(entity.level()
                instanceof ServerLevel level)) {
            return;
        }

        entity.getPersistentData()
                .putBoolean(
                        TAG_FINALIZED,
                        true
                );

        Set<UUID> existingHearts =
                context.mode().isNullification()
                        ? snapshotGuardianHearts(level)
                        : Set.of();

        Object fightManager =
                ReflectionUtil.invoke(
                        entity,
                        new String[]{
                                "getFightManager"
                        }
                );

        /*
         * 直接调用 Entity#kill。
         *
         * Java 虚方法分派会进入 DraconicGuardianEntity 自己覆盖的 kill，
         * 从而调用 processDragonDeath。这里没有硬链接 DE 的任何类。
         */
        try {
            entity.kill();
        } catch (Throwable ignored) {
        }

        /*
         * 极端情况下，其他核心修改可能吞掉 kill。
         * 此时只对已确认的 GuardianFightManager 调用正式死亡结算。
         */
        if (!entity.isRemoved()
                && fightManager != null
                && MANAGER_CLASS.equals(
                fightManager.getClass().getName()
        )) {
            ReflectionUtil.invokeVoid(
                    fightManager,
                    new String[]{
                            "processDragonDeath"
                    },
                    entity
            );
        }

        /*
         * 毁灭模式保留混沌守卫自己的龙心奖励。
         * 归零模式仍然必须无奖励，因此只删除这次正式结算中新生成的龙心。
         */
        if (context.mode().isNullification()) {
            removeNewGuardianHearts(
                    level,
                    existingHearts
            );
        }
    }

    private static boolean isChaosGuardian(
            Entity entity
    ) {
        if (GUARDIAN_CLASS.equals(
                entity.getClass().getName()
        )) {
            return true;
        }

        Object fightManager =
                ReflectionUtil.invoke(
                        entity,
                        new String[]{
                                "getFightManager"
                        }
                );

        return fightManager != null
                && MANAGER_CLASS.equals(
                fightManager.getClass().getName()
        );
    }

    private static Set<UUID> snapshotGuardianHearts(
            ServerLevel level
    ) {
        Set<UUID> result =
                new HashSet<>();

        for (Entity candidate
                : level.getAllEntities()) {
            if (candidate
                    instanceof ItemEntity item
                    && item.getPersistentData()
                    .getBoolean(
                            GUARDIAN_HEART_TAG
                    )) {
                result.add(
                        item.getUUID()
                );
            }
        }

        return result;
    }

    private static void removeNewGuardianHearts(
            ServerLevel level,
            Set<UUID> existingHearts
    ) {
        for (Entity candidate
                : level.getAllEntities()) {
            if (!(candidate
                    instanceof ItemEntity item)
                    || existingHearts.contains(
                    item.getUUID()
            )
                    || !item.getPersistentData()
                    .getBoolean(
                            GUARDIAN_HEART_TAG
                    )) {
                continue;
            }

            try {
                item.discard();
            } catch (Throwable ignored) {
            }
        }
    }
}
