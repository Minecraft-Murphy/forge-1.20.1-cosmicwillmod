package com.Murphy.cosmicwill.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class AlwaysFoilBlockItem extends BlockItem {

    public AlwaysFoilBlockItem(
            Block block,
            Properties properties
    ) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
