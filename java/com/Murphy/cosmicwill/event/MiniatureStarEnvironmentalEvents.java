package com.Murphy.cosmicwill.event;

import com.Murphy.cosmicwill.CustomWill;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MiniatureStarEnvironmentalEvents {

    /*
     * 以恒星为中心半径 7，即 15×15×15 范围。
     */
    private static final int MONSTER_SUPPRESSION_RADIUS = 7;

    private MiniatureStarEnvironmentalEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(
            MobSpawnEvent.FinalizeSpawn event
    ) {
        MobSpawnType spawnType =
                event.getSpawnType();

        /*
         * 只阻止自然与区块生成。
         * 刷怪笼、命令、结构脚本、繁殖和玩家召唤不受影响。
         */
        if (spawnType != MobSpawnType.NATURAL
                && spawnType
                != MobSpawnType.CHUNK_GENERATION) {
            return;
        }

        boolean hostile =
                event.getEntity()
                        instanceof Enemy
                        || event.getEntity()
                        .getType()
                        .getCategory()
                        == MobCategory.MONSTER;

        if (!hostile) {
            return;
        }

        ServerLevel level =
                event.getLevel()
                        .getLevel();

        BlockPos spawnPos =
                BlockPos.containing(
                        event.getX(),
                        event.getY(),
                        event.getZ()
                );

        if (MiniatureStarInfluence
                .hasActiveStarWithin(
                        level,
                        spawnPos,
                        MONSTER_SUPPRESSION_RADIUS
                )) {
            /*
             * Forge 1.20.1 要真正阻止实体进入世界，
             * 必须使用 setSpawnCancelled，而不是只取消事件。
             */
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(
            LevelEvent.Unload event
    ) {
        if (event.getLevel()
                instanceof ServerLevel level) {
            MiniatureStarInfluence.clear(
                    level
            );
        }
    }
}
