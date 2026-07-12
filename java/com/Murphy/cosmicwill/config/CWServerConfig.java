package com.Murphy.cosmicwill.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CWServerConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.LongValue MATTER_CAPACITY;
    public static final ForgeConfigSpec.IntValue ITEMS_PER_TICK;

    public static final ForgeConfigSpec.IntValue
            COMPRESSED_FURNACE_SPEED_MULTIPLIER;

    /**
     * false：
     * 归零模式抹除玩家时保留背包、盔甲、主副手和光标物品。
     *
     * true：
     * 保留旧版行为，归零时清空玩家的全部物品。
     */
    public static final ForgeConfigSpec.BooleanValue
            NULLIFICATION_DELETES_PLAYER_EQUIPMENT;

    static {
        ForgeConfigSpec.Builder builder =
                new ForgeConfigSpec.Builder();

        builder.push("matter_deconstructor");

        MATTER_CAPACITY = builder
                .comment(
                        "物质解构器默认最大 MU 容量。",
                        "内部使用 long，不受 int 上限限制。"
                )
                .defineInRange(
                        "matterCapacity",
                        10_000_000L,
                        1L,
                        Long.MAX_VALUE
                );

        ITEMS_PER_TICK = builder
                .comment(
                        "物质解构器每个游戏刻最多处理多少件物品。",
                        "默认 64，即每秒最多产生 1280 MU。"
                )
                .defineInRange(
                        "itemsPerTick",
                        64,
                        1,
                        4096
                );

        builder.pop();

        builder.push("compressed_furnace");

        COMPRESSED_FURNACE_SPEED_MULTIPLIER = builder
                .comment(
                        "压缩熔炉相对于原版熔炉的运行倍率。",
                        "燃料和烧制进度会同比加速，因此单位产物燃料消耗不变。"
                )
                .defineInRange(
                        "speedMultiplier",
                        4,
                        1,
                        64
                );

        builder.pop();

        builder.push("nullifier");

        NULLIFICATION_DELETES_PLAYER_EQUIPMENT = builder
                .comment(
                        "归零者的两种归零模式命中玩家时，是否删除其全部物品。",
                        "包括背包、盔甲、主副手及鼠标光标物品。",
                        "默认 false：玩家仍会被归零，但物品不会被删除。"
                )
                .define(
                        "deletePlayerEquipmentOnNullification",
                        false
                );

        builder.pop();

        SPEC = builder.build();
    }

    private CWServerConfig() {
    }
}
