package com.Murphy.cosmicwill.nullification;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.damage.CWDamageTypes;
import com.Murphy.cosmicwill.items.NullifierSwordItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class NullificationEvents {

    private NullificationEvents() {
    }

    @SubscribeEvent
    public static void onServerLevelTick(
            TickEvent.LevelTickEvent event
    ) {
        if (event.phase == TickEvent.Phase.END
                && event.level
                instanceof ServerLevel serverLevel) {
            NullificationManager.tick(
                    serverLevel
            );
        }
    }

    /*
     * 非玩家实体的归零左键仍需等待一个 Tick，
     * 因此期间阻止其进入普通受伤/死亡脚本。
     *
     * 玩家归零由下方的专用硬处决链处理，不能在这里提前取消。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(
            LivingAttackEvent event
    ) {
        if (NullificationManager.isNullified(
                event.getEntity()
        )
                && !NullificationManager
                .isPlayerErasurePending(
                        event.getEntity()
                )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(
            LivingHurtEvent event
    ) {
        if (NullificationManager.isNullified(
                event.getEntity()
        )
                && !NullificationManager
                .isPlayerErasurePending(
                        event.getEntity()
                )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHeal(
            LivingHealEvent event
    ) {
        if (NullificationManager.isNullified(
                event.getEntity()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(
            LivingDeathEvent event
    ) {
        if (NullificationManager.isNullified(
                event.getEntity()
        )
                && !NullificationManager
                .isPlayerErasurePending(
                        event.getEntity()
                )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST,
            receiveCanceled = true
    )
    public static void forcePlayerAttack(
            LivingAttackEvent event
    ) {
        if (isHardPlayerErasure(
                event.getEntity(),
                event.getSource()
        )) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST,
            receiveCanceled = true
    )
    public static void forcePlayerHurt(
            LivingHurtEvent event
    ) {
        if (isHardPlayerErasure(
                event.getEntity(),
                event.getSource()
        )) {
            event.setCanceled(false);
            event.setAmount(
                    Float.MAX_VALUE
            );
        }
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST,
            receiveCanceled = true
    )
    public static void forcePlayerDamage(
            LivingDamageEvent event
    ) {
        if (isHardPlayerErasure(
                event.getEntity(),
                event.getSource()
        )) {
            event.setCanceled(false);
            event.setAmount(
                    Float.MAX_VALUE
            );

            NullificationManager.forceVisualHealth(
                    event.getEntity(),
                    0.0F
            );
        }
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST,
            receiveCanceled = true
    )
    public static void forcePlayerDeath(
            LivingDeathEvent event
    ) {
        if (!isHardPlayerErasure(
                event.getEntity(),
                event.getSource()
        )) {
            return;
        }

        event.setCanceled(false);

        NullificationManager.forceVisualHealth(
                event.getEntity(),
                0.0F
        );

        try {
            event.getEntity()
                    .setHealth(0.0F);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isHardPlayerErasure(
            LivingEntity entity,
            DamageSource source
    ) {
        return entity instanceof ServerPlayer
                && (CWDamageTypes
                .isNullification(source)
                || NullificationManager
                .isPlayerErasurePending(entity));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(
            LivingDropsEvent event
    ) {
        if (NullificationManager.hasMark(
                event.getEntity(),
                NullificationManager.TAG_NO_DROPS
        )) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExperienceDrop(
            LivingExperienceDropEvent event
    ) {
        if (NullificationManager.hasMark(
                event.getEntity(),
                NullificationManager.TAG_NO_DROPS
        )) {
            event.setDroppedExperience(0);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(
            EntityJoinLevelEvent event
    ) {
        Entity entity =
                event.getEntity();

        if (NullificationManager.isNullified(
                entity
        )
                && !(entity
                instanceof ServerPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMultipartEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {
        Entity rawTarget =
                event.getTarget();

        if (!(rawTarget
                instanceof PartEntity<?> part)) {
            return;
        }

        ItemStack stack =
                event.getItemStack();

        if (!(stack.getItem()
                instanceof NullifierSwordItem)) {
            return;
        }

        Entity parent =
                part.getParent();

        if (parent == null
                || parent == event.getEntity()) {
            return;
        }

        if (event.getEntity()
                .level()
                .isClientSide) {
            event.setCancellationResult(
                    InteractionResult.SUCCESS
            );
            event.setCanceled(true);
            return;
        }

        if (!(event.getEntity()
                instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (serverPlayer.isShiftKeyDown()) {
            NullifierSwordItem
                    .toggleModeAndNotify(
                            stack,
                            serverPlayer
                    );
        } else if (NullifierSwordItem
                .getMode(stack)
                == NullifierSwordItem
                .MODE_DESTRUCTION) {
            NullifierTargeting
                    .performDestructionSweep(
                            serverPlayer,
                            parent
                    );
        } else {
            NullificationManager
                    .conceptErase(
                            parent,
                            serverPlayer
                    );
        }

        serverPlayer.swing(
                event.getHand(),
                true
        );

        event.setCancellationResult(
                InteractionResult.CONSUME
        );

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLevelUnload(
            LevelEvent.Unload event
    ) {
        if (event.getLevel()
                instanceof ServerLevel serverLevel) {
            NullificationManager
                    .onLevelUnload(
                            serverLevel
                    );
        }
    }
}
