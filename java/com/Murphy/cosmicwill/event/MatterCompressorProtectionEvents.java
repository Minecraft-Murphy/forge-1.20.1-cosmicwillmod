package com.Murphy.cosmicwill.event;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.block.MatterCompressorBlock;
import com.Murphy.cosmicwill.registry.CWBlocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = CustomWill.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MatterCompressorProtectionEvents {

    private MatterCompressorProtectionEvents() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {
        if (event.getEntity().isCreative()) {
            return;
        }

        if (isSingularityCompressor(
                event.getState()
        )) {
            event.setNewSpeed(0.0F);
        }
    }

    /**
     * 生存模式无法破坏奇点压缩器。
     *
     * 创造模式允许破坏；方块真正被移除时，
     * MatterCompressorBlock#onRemove 会触发奇点灾变爆炸。
     */
    @SubscribeEvent
    public static void onBreak(
            BlockEvent.BreakEvent event
    ) {
        Player player = event.getPlayer();

        if (player != null
                && player.isCreative()) {
            return;
        }

        if (isSingularityCompressor(
                event.getState()
        )) {
            event.setCanceled(true);
        }
    }

    private static boolean isSingularityCompressor(
            BlockState state
    ) {
        return state.is(
                CWBlocks.MATTER_COMPRESSOR.get()
        )
                && state.hasProperty(
                MatterCompressorBlock.SINGULARITY
        )
                && state.getValue(
                MatterCompressorBlock.SINGULARITY
        );
    }
}
