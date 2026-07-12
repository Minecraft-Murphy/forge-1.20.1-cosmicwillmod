package com.Murphy.cosmicwill.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public final class MiniatureStarBlockItem extends BlockItem {

    public MiniatureStarBlockItem(
            Block block,
            Properties properties
    ) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(
                getDescriptionId(stack)
        ).withStyle(ChatFormatting.RED);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
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
                        "tooltip.cosmicwill.miniature_star.annihilation"
                ).withStyle(ChatFormatting.RED)
        );

        tooltip.add(
                Component.translatable(
                        "tooltip.cosmicwill.miniature_star.warning"
                ).withStyle(ChatFormatting.RED)
        );

        super.appendHoverText(
                stack,
                level,
                tooltip,
                flag
        );
    }
}
