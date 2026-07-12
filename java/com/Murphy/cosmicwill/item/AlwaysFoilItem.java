package com.Murphy.cosmicwill.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AlwaysFoilItem extends Item {

    public AlwaysFoilItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
