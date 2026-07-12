package com.Murphy.cosmicwill.nullification;

import com.Murphy.cosmicwill.compat.CompatDispatcher;
import com.Murphy.cosmicwill.config.CWServerConfig;
import com.Murphy.cosmicwill.damage.CWDamageTypes;
import com.Murphy.cosmicwill.mixins.LivingEntityAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NullificationManager {

    public static final String TAG_ERASURE_ROOT = "cosmicwill:absolute_erasure_root";
    public static final String TAG_REWARD_SETTLED = "cosmicwill:reward_settled";
    public static final String TAG_REWARD_SETTLING = "cosmicwill:reward_settling";
    public static final String TAG_RULE_NULLIFIED = "cosmicwill:rule_nullified";
    public static final String TAG_CONCEPT_ERASED = "cosmicwill:concept_erased";
    public static final String TAG_NO_DROPS = "cosmicwill:no_drops";

    private static final Map<ServerLevel, List<ErasureContext>> ACTIVE_CONTEXTS =
            new HashMap<>();
    private static final Map<ServerLevel, List<PlayerEraseTask>> PLAYER_TASKS =
            new HashMap<>();

    private NullificationManager() {
    }


    public static void destructionSingle(Entity target, ServerPlayer attacker) {
        begin(target, attacker, ErasureMode.DESTRUCTION_SINGLE);
    }

    public static void destructionSweep(Entity target, ServerPlayer attacker) {
        begin(target, attacker, ErasureMode.DESTRUCTION_SWEEP);
    }

    public static void ruleNullify(Entity target, ServerPlayer attacker) {
        begin(target, attacker, ErasureMode.RULE_NULLIFICATION);
    }

    public static void conceptErase(Entity target, ServerPlayer attacker) {
        begin(target, attacker, ErasureMode.CONCEPT_ERASURE);
    }

    public static void settleAndDestroy(Entity target, ServerPlayer attacker) {
        destructionSingle(target, attacker);
    }

    private static void begin(
            Entity rawTarget,
            ServerPlayer attacker,
            ErasureMode mode
    ) {
        Entity target = NullifierTargeting.resolveParent(rawTarget);
        if (target == null || target == attacker) {
            return;
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }

        /*
         * 只在根目标上生成一次处决特效。
         * 保留此前已经完成的毁灭白色湮灭粒子与归零黑雾效果，
         * 不对子实体和配对实体重复生成粒子风暴。
         */
        if (mode.isNullification()) {
            NullifierKillEffects.spawnNullification(target);
        } else {
            NullifierKillEffects.spawnDestruction(target);
        }

        if (target instanceof ServerPlayer playerTarget) {
            handlePlayer(playerTarget, attacker, mode);
            return;
        }

        if (mode.settlesRewards() && target.getClass() == EnderDragon.class) {
            prepareVanillaDragon((EnderDragon) target, attacker);
            return;
        }

        ErasureContext context = new ErasureContext(target, mode);
        ACTIVE_CONTEXTS.computeIfAbsent(level, ignored -> new ArrayList<>())
                .add(context);

        markRoot(target, mode);
        ErasureLedger.markErased(target);
        CompatDispatcher.beforeErase(context, target);

        for (Entity paired : CompatDispatcher.collectPairedEntities(target)) {
            if (paired == null || paired == target || paired instanceof ServerPlayer) {
                continue;
            }
            context.addLinked(paired);
            eraseLinked(context, paired);
        }

        eraseParts(context, target);
        cleanupAuxiliaries(context, snapshot(level));

        if (mode.settlesRewards() && target instanceof LivingEntity living) {
            RewardSettlement.settleOnce(level, living, attacker);
        }

        if (mode == ErasureMode.RULE_NULLIFICATION) {
            if (target instanceof LivingEntity living) {
                forceVisualHealth(living, 0.0F);
                try {
                    level.broadcastEntityEvent(living, (byte) 2);
                } catch (Throwable ignored) {
                }
            }
            return;
        }

        eraseRoot(level, context, target);
    }

    private static void markRoot(Entity target, ErasureMode mode) {
        target.getPersistentData().putBoolean(TAG_ERASURE_ROOT, true);
        target.getPersistentData().putBoolean(TAG_NO_DROPS, mode.isNullification());
        if (mode == ErasureMode.RULE_NULLIFICATION) {
            target.getPersistentData().putBoolean(TAG_RULE_NULLIFIED, true);
        }
        if (mode == ErasureMode.CONCEPT_ERASURE) {
            target.getPersistentData().putBoolean(TAG_CONCEPT_ERASED, true);
        }
    }

    private static void eraseRoot(
            ServerLevel level,
            ErasureContext context,
            Entity target
    ) {
        if (target instanceof ServerPlayer) {
            return;
        }
        markRoot(target, context.mode());
        ErasureLedger.markErased(target);
        CompatDispatcher.beforeErase(context, target);

        /*
         * 在真正 discard 之前完成外部 Boss 自己的战斗状态结算。
         * 对混沌守卫而言，这一步会正式关闭 GuardianFightManager，
         * 防止 1200 Tick 后重新生成。
         */
        CompatDispatcher.beforeRootErase(
                context,
                target
        );

        EntityRegistryEraser.erase(
                level,
                target,
                context
        );
    }

    private static void eraseLinked(ErasureContext context, Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || entity instanceof ServerPlayer) {
            return;
        }
        entity.getPersistentData().putBoolean(TAG_ERASURE_ROOT, true);
        entity.getPersistentData().putBoolean(TAG_CONCEPT_ERASED, true);
        entity.getPersistentData().putBoolean(TAG_NO_DROPS, true);
        ErasureLedger.markErased(entity);
        CompatDispatcher.beforeErase(context, entity);
        EntityRegistryEraser.erase(level, entity, context);
    }

    public static void tick(ServerLevel level) {
        tickPlayers(level);

        List<ErasureContext> contexts = ACTIVE_CONTEXTS.get(level);
        if (contexts == null || contexts.isEmpty()) {
            ErasureLedger.purgeExpired();
            return;
        }

        List<Entity> entities = snapshot(level);
        Iterator<ErasureContext> iterator = contexts.iterator();

        while (iterator.hasNext()) {
            ErasureContext context = iterator.next();
            context.incrementAge();

            Entity pending;
            while ((pending = context.pollPending()) != null) {
                eraseLinked(context, pending);
            }

            if (context.age() <= 24 || context.age() % 5 == 0) {
                cleanupAuxiliaries(context, entities);
            }

            if (context.delayElapsed()) {
                eraseKnownRootReferences(level, context, entities);
            }

            if (context.isExpired()) {
                iterator.remove();
            }
        }

        if (contexts.isEmpty()) {
            ACTIVE_CONTEXTS.remove(level);
        }
        ErasureLedger.purgeExpired();
    }

    private static void eraseKnownRootReferences(
            ServerLevel level,
            ErasureContext context,
            List<Entity> entities
    ) {
        Entity rootReference = context.rootReference();
        if (rootReference != null) {
            eraseRoot(level, context, rootReference);
        }

        try {
            Entity byUuid = level.getEntity(context.rootUuid());
            if (byUuid != null) {
                eraseRoot(level, context, byUuid);
            }
        } catch (Throwable ignored) {
        }


        for (Entity candidate : entities) {
            if (candidate instanceof ServerPlayer) {
                continue;
            }
            if (context.matchesReplacement(candidate)) {
                context.addLinked(candidate);
                eraseRoot(level, context, candidate);
            }
        }
    }

    private static void cleanupAuxiliaries(
            ErasureContext context,
            List<Entity> entities
    ) {
        for (Entity candidate : entities) {
            if (candidate instanceof ServerPlayer
                    || candidate.getUUID().equals(context.rootUuid())) {
                continue;
            }
            if (CompatDispatcher.isAuxiliary(context, candidate)) {
                context.addLinked(candidate);
                eraseLinked(context, candidate);
            }
        }
    }

    private static void eraseParts(ErasureContext context, Entity target) {
        PartEntity<?>[] parts;
        try {
            parts = target.getParts();
        } catch (Throwable ignored) {
            return;
        }
        if (parts == null) {
            return;
        }
        for (PartEntity<?> part : parts) {
            if (part != null) {
                context.addLinked(part);
                eraseLinked(context, part);
            }
        }
    }


    public static boolean interceptEntityJoin(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || entity instanceof ServerPlayer) {
            return false;
        }
        List<ErasureContext> contexts = ACTIVE_CONTEXTS.get(level);
        if (contexts == null || contexts.isEmpty()) {
            return false;
        }

        for (ErasureContext context : contexts) {
            if (!context.matchesReplacement(entity)
                    && !CompatDispatcher.isAuxiliary(context, entity)) {
                continue;
            }

            context.addLinked(entity);
            entity.getPersistentData().putBoolean(TAG_ERASURE_ROOT, true);
            entity.getPersistentData().putBoolean(TAG_CONCEPT_ERASED, true);
            entity.getPersistentData().putBoolean(TAG_NO_DROPS, true);
            ErasureLedger.markErased(entity);

            try {
                entity.discard();
            } catch (Throwable ignored) {
            }
            return true;
        }
        return false;
    }

    public static void onLevelUnload(ServerLevel level) {
        ACTIVE_CONTEXTS.remove(level);
        PLAYER_TASKS.remove(level);
    }

    private static List<Entity> snapshot(ServerLevel level) {
        List<Entity> entities = new ArrayList<>();
        try {
            for (Entity entity : level.getAllEntities()) {
                entities.add(entity);
            }
        } catch (Throwable ignored) {
        }
        return entities;
    }


    private static void handlePlayer(
            ServerPlayer victim,
            ServerPlayer attacker,
            ErasureMode mode
    ) {
        if (victim == attacker) {
            return;
        }

        ServerLevel level =
                (ServerLevel) victim.level();

        if (mode == ErasureMode.DESTRUCTION_SINGLE
                || mode == ErasureMode.DESTRUCTION_SWEEP) {
            /*
             * 毁灭模式保留强度梯度：
             * 仍然走普通玩家死亡流程，因此无尽套、终末之环、
             * 创造之心和其他高阶防具可以继续免疫或抵消。
             */
            DamageSource source =
                    victim.damageSources()
                            .playerAttack(attacker);

            preparePlayerKillCredit(
                    victim,
                    attacker
            );

            forceVisualHealth(
                    victim,
                    0.0F
            );

            victim.die(source);
            return;
        }

        boolean deleteEquipment =
                CWServerConfig
                        .NULLIFICATION_DELETES_PLAYER_EQUIPMENT
                        .get();

        victim.getPersistentData()
                .putBoolean(
                        mode == ErasureMode.RULE_NULLIFICATION
                                ? TAG_RULE_NULLIFIED
                                : TAG_CONCEPT_ERASED,
                        true
                );

        /*
         * 默认 false 时不设置 TAG_NO_DROPS，
         * 玩家物品遵循正常 keepInventory / 死亡掉落规则；
         * 开启配置后才在死亡前真正清空并禁止掉落。
         */
        if (deleteEquipment) {
            victim.getPersistentData()
                    .putBoolean(
                            TAG_NO_DROPS,
                            true
                    );
        } else {
            victim.getPersistentData()
                    .remove(TAG_NO_DROPS);
        }

        int delay =
                mode == ErasureMode.RULE_NULLIFICATION
                        ? 1
                        : 0;

        if (mode == ErasureMode.RULE_NULLIFICATION) {
            forceVisualHealth(
                    victim,
                    0.0F
            );

            try {
                level.broadcastEntityEvent(
                        victim,
                        (byte) 2
                );
            } catch (Throwable ignored) {
            }
        }

        schedulePlayerErasure(
                level,
                victim,
                attacker,
                delay
        );

        /*
         * 右键概念归零立即执行第一次尝试；
         * 后续世界 Tick 仍会重复加固，防止其他模组在同一事件链中复活。
         */
        if (delay == 0) {
            enforcePlayerErasure(
                    victim,
                    attacker
            );
        }
    }

    private static void schedulePlayerErasure(
            ServerLevel level,
            ServerPlayer victim,
            ServerPlayer attacker,
            int delay
    ) {
        List<PlayerEraseTask> tasks =
                PLAYER_TASKS.computeIfAbsent(
                        level,
                        ignored -> new ArrayList<>()
                );

        tasks.removeIf(task ->
                task.playerUuid.equals(
                        victim.getUUID()
                )
        );

        tasks.add(
                new PlayerEraseTask(
                        victim.getUUID(),
                        attacker.getUUID(),
                        delay
                )
        );
    }

    private static void preparePlayerKillCredit(
            ServerPlayer victim,
            ServerPlayer attacker
    ) {
        try {
            victim.setLastHurtByPlayer(attacker);
            victim.setLastHurtByMob(attacker);
        } catch (Throwable ignored) {
        }
    }

    private static void tickPlayers(
            ServerLevel level
    ) {
        List<PlayerEraseTask> tasks =
                PLAYER_TASKS.get(level);

        if (tasks == null
                || tasks.isEmpty()) {
            return;
        }

        Iterator<PlayerEraseTask> iterator =
                tasks.iterator();

        while (iterator.hasNext()) {
            PlayerEraseTask task =
                    iterator.next();

            task.age++;

            if (task.age <= task.delay) {
                continue;
            }

            ServerPlayer player =
                    level.getServer()
                            .getPlayerList()
                            .getPlayer(
                                    task.playerUuid
                            );

            if (player == null) {
                iterator.remove();
                continue;
            }

            ServerPlayer attacker =
                    level.getServer()
                            .getPlayerList()
                            .getPlayer(
                                    task.attackerUuid
                            );

            enforcePlayerErasure(
                    player,
                    attacker
            );

            task.attempts++;

            if (player.isDeadOrDying()
                    || player.getHealth() <= 0.0F) {
                task.confirmedDeadTicks++;
            } else {
                task.confirmedDeadTicks = 0;
            }

            /*
             * 至少在两个服务端 Tick 中确认死亡后再断线，
             * 避免像旧版那样只断开连接、角色实际仍被饰品复活。
             */
            if (task.confirmedDeadTicks >= 2) {
                player.connection.disconnect(
                        Component.empty()
                );

                iterator.remove();
                continue;
            }

            /*
             * 极端防御模组可能在 Tick 末尾再次抬血。
             * 保留最多一秒的重复处决窗口；到达上限仍会在
             * 归零标记保留下继续强制 0 血后断开连接。
             */
            if (task.attempts >= 20) {
                forceVisualHealth(
                        player,
                        0.0F
                );

                try {
                    player.setHealth(0.0F);
                } catch (Throwable ignored) {
                }

                player.connection.disconnect(
                        Component.empty()
                );

                iterator.remove();
            }
        }

        if (tasks.isEmpty()) {
            PLAYER_TASKS.remove(level);
        }
    }

    private static void enforcePlayerErasure(
            ServerPlayer player,
            @Nullable ServerPlayer attacker
    ) {
        boolean deleteEquipment =
                CWServerConfig
                        .NULLIFICATION_DELETES_PLAYER_EQUIPMENT
                        .get();

        if (deleteEquipment) {
            player.getInventory()
                    .clearContent();

            player.containerMenu.setCarried(
                    ItemStack.EMPTY
            );

            player.getInventory()
                    .setChanged();

            player.inventoryMenu
                    .broadcastChanges();

            player.containerMenu
                    .broadcastChanges();
        }

        /*
         * 通用硬实力层：
         * 不识别具体模组物品，不依赖 Curios，也不写实体名映射。
         */
        player.setInvulnerable(false);
        player.getAbilities().invulnerable = false;
        player.onUpdateAbilities();

        player.invulnerableTime = 0;
        player.hurtTime = 0;
        player.hurtDuration = 0;

        try {
            player.setAbsorptionAmount(0.0F);
        } catch (Throwable ignored) {
        }

        if (attacker != null) {
            preparePlayerKillCredit(
                    player,
                    attacker
            );
        }

        DamageSource source =
                CWDamageTypes.nullification(
                        player.level(),
                        attacker
                );

        /*
         * 先走一次完整伤害链，允许死亡统计、战斗记录和客户端反馈；
         * NullificationEvents 会在 Forge 最低优先级撤销其他模组
         * 对该专用来源的事件取消。
         */
        try {
            player.hurt(
                    source,
                    Float.MAX_VALUE
            );
        } catch (Throwable ignored) {
        }

        /*
         * 再直接压低同步生命并调用 die。
         * 即便某个护具在 Hurt/Damage 阶段把生命恢复到 1，
         * Death 阶段仍会再次被执行。
         */
        forceVisualHealth(
                player,
                0.0F
        );

        try {
            player.setHealth(0.0F);
        } catch (Throwable ignored) {
        }

        try {
            player.die(source);
        } catch (Throwable ignored) {
        }

        forceVisualHealth(
                player,
                0.0F
        );
    }

    public static boolean isPlayerErasurePending(
            Entity entity
    ) {
        if (!(entity
                instanceof ServerPlayer player)
                || !(player.level()
                instanceof ServerLevel level)) {
            return false;
        }

        List<PlayerEraseTask> tasks =
                PLAYER_TASKS.get(level);

        if (tasks == null) {
            return false;
        }

        UUID playerUuid =
                player.getUUID();

        for (PlayerEraseTask task : tasks) {
            if (task.playerUuid.equals(
                    playerUuid
            )) {
                return true;
            }
        }

        return false;
    }

    private static void prepareVanillaDragon(
            EnderDragon dragon,
            ServerPlayer attacker
    ) {
        try {
            dragon.setLastHurtByPlayer(attacker);
            dragon.setLastHurtByMob(attacker);
        } catch (Throwable ignored) {
        }
        forceVisualHealth(dragon, 0.0F);
        dragon.getPhaseManager().setPhase(EnderDragonPhase.DYING);
    }

    public static void forceVisualHealth(LivingEntity living, float health) {
        try {
            living.getEntityData().set(
                    LivingEntityAccessor.cosmicwill$getHealthDataAccessor(),
                    health
            );
        } catch (Throwable ignored) {
            try {
                living.setHealth(health);
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    public static boolean hasMark(Entity entity, String key) {
        return entity.getPersistentData().getBoolean(key);
    }

    public static boolean isNullified(Entity entity) {
        return hasMark(entity, TAG_RULE_NULLIFIED)
                || hasMark(entity, TAG_CONCEPT_ERASED);
    }

    private static final class PlayerEraseTask {
        private final UUID playerUuid;
        private final UUID attackerUuid;
        private final int delay;

        private int age;
        private int attempts;
        private int confirmedDeadTicks;

        private PlayerEraseTask(
                UUID playerUuid,
                UUID attackerUuid,
                int delay
        ) {
            this.playerUuid = playerUuid;
            this.attackerUuid = attackerUuid;
            this.delay = delay;
        }
    }
}
