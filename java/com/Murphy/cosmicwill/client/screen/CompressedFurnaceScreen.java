package com.Murphy.cosmicwill.client.screen;

import com.Murphy.cosmicwill.menu.CompressedFurnaceMenu;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Objects;

public final class CompressedFurnaceScreen
        extends AbstractFurnaceScreen<CompressedFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            Objects.requireNonNull(
                    ResourceLocation.tryBuild(
                            "minecraft",
                            "textures/gui/container/furnace.png"
                    )
            );

    public CompressedFurnaceScreen(
            CompressedFurnaceMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                new SmeltingRecipeBookComponent(),
                inventory,
                title,
                TEXTURE
        );
    }
}
