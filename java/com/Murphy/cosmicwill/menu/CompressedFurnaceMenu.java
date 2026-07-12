package com.Murphy.cosmicwill.menu;

import com.Murphy.cosmicwill.registry.CWMenus;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeType;

public final class CompressedFurnaceMenu
        extends AbstractFurnaceMenu {

    public CompressedFurnaceMenu(
            int containerId,
            Inventory inventory
    ) {
        super(
                CWMenus.COMPRESSED_FURNACE.get(),
                RecipeType.SMELTING,
                RecipeBookType.FURNACE,
                containerId,
                inventory
        );
    }

    public CompressedFurnaceMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(
                CWMenus.COMPRESSED_FURNACE.get(),
                RecipeType.SMELTING,
                RecipeBookType.FURNACE,
                containerId,
                inventory,
                container,
                data
        );
    }
}
