package com.Murphy.cosmicwill.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public final class SealedChargedEnergyBladeItem
        extends Item {

    public SealedChargedEnergyBladeItem(
            Properties properties
    ) {
        super(properties);
    }


    @Override
    public boolean onBlockStartBreak(
            ItemStack stack,
            BlockPos pos,
            Player player
    ) {
        BlockState state =
                player.level().getBlockState(pos);

        if (!state.is(Blocks.BEDROCK)) {
            return false;
        }


        if (player.level().isClientSide) {
            return false;
        }

        if (!(player.level()
                instanceof ServerLevel serverLevel)) {
            return true;
        }

        Block.popResource(
                serverLevel,
                pos,
                new ItemStack(Blocks.BEDROCK)
        );

        serverLevel.removeBlockEntity(pos);
        serverLevel.setBlock(
                pos,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL
                        | Block.UPDATE_SUPPRESS_DROPS
        );

        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.sealed_blade.no_damage"
                ).withStyle(ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.sealed_blade.bedrock"
                ).withStyle(ChatFormatting.DARK_GRAY)
        );


        super.appendHoverText(
                stack,
                level,
                tooltip,
                flag
        );
    }
}
