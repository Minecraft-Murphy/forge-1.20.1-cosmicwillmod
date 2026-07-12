package com.Murphy.cosmicwill.event;

import com.Murphy.cosmicwill.CustomWill;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

/**
 * “永恒”掉落物属性。
 *
 * 标签中的物品变成 ItemEntity 后：
 * - 无限寿命，不会按普通掉落物的五分钟规则消失；
 * - 实体无敌，仙人掌、火焰、熔岩、雷击及普通伤害不能摧毁；
 * - 定期清除燃烧状态；
 * - 仍可被玩家正常拾取。
 */
@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class EternalItemEvents {

    private static final TagKey<Item> ETERNAL_ITEMS =
            TagKey.create(
                    Registries.ITEM,
                    Objects.requireNonNull(
                            ResourceLocation.tryBuild(
                                    CustomWill.MODID,
                                    "eternal_items"
                            )
                    )
            );

    private static final String ETERNAL_MARK =
            CustomWill.MODID + ":eternal_item";

    private EternalItemEvents() {
    }

    /**
     * 新掉落或从区块 NBT 重新加载时立即应用。
     */
    @SubscribeEvent(
            priority = EventPriority.HIGHEST
    )
    public static void onEntityJoin(
            EntityJoinLevelEvent event
    ) {
        if (event.getLevel().isClientSide
                || !(event.getEntity()
                instanceof ItemEntity itemEntity)
                || !isEternal(itemEntity)) {
            return;
        }

        applyEternalState(itemEntity);
    }

    /**
     * 每秒加固一次状态。
     *
     * 这样即使其他模组修改掉落物年龄、无敌标志或点燃它，
     * 这些物品仍会恢复永恒状态。
     */
    @SubscribeEvent
    public static void onLevelTick(
            TickEvent.LevelTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END
                || !(event.level
                instanceof ServerLevel level)
                || level.getGameTime() % 20L != 0L) {
            return;
        }

        for (Entity entity
                : level.getAllEntities()) {
            if (!(entity
                    instanceof ItemEntity itemEntity)
                    || !isEternal(itemEntity)) {
                continue;
            }

            applyEternalState(itemEntity);
        }
    }

    private static boolean isEternal(
            ItemEntity itemEntity
    ) {
        return itemEntity.getItem()
                .is(ETERNAL_ITEMS);
    }

    private static void applyEternalState(
            ItemEntity itemEntity
    ) {
        itemEntity.setUnlimitedLifetime();
        itemEntity.setInvulnerable(true);
        itemEntity.clearFire();

        itemEntity.getPersistentData()
                .putBoolean(
                        ETERNAL_MARK,
                        true
                );
    }
}
