package com.Murphy.cosmicwill.blockentity;

import com.Murphy.cosmicwill.menu.CompressedFurnaceMenu;
import com.Murphy.cosmicwill.mixins.AbstractFurnaceBlockEntityAccessor;
import com.Murphy.cosmicwill.registry.CWBlockEntities;
import com.Murphy.cosmicwill.registry.CWBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class CompressedFurnaceBlockEntity
        extends AbstractFurnaceBlockEntity {

    private int stellarHeatTicks;

    private boolean stellarHeatActive;
    private int storedLitTime;
    private int storedLitDuration;

    public CompressedFurnaceBlockEntity(
            BlockPos pos,
            BlockState state
    ) {

        super(
                CWBlockEntities.COMPRESSED_FURNACE.get(),
                pos,
                state,
                RecipeType.SMELTING
        );

        installCombinedCookingCheck();
    }

    private void installCombinedCookingCheck() {
        RecipeManager.CachedCheck<
                Container,
                AbstractCookingRecipe
                > combinedCheck =
                (container, level) -> {
                    Optional<BlastingRecipe> blasting =
                            level.getRecipeManager()
                                    .getRecipeFor(
                                            RecipeType.BLASTING,
                                            container,
                                            level
                                    );

                    if (blasting.isPresent()) {
                        return Optional.of(
                                (AbstractCookingRecipe)
                                        blasting.get()
                        );
                    }

                    Optional<SmeltingRecipe> smelting =
                            level.getRecipeManager()
                                    .getRecipeFor(
                                            RecipeType.SMELTING,
                                            container,
                                            level
                                    );

                    if (smelting.isPresent()) {
                        return Optional.of(
                                (AbstractCookingRecipe)
                                        smelting.get()
                        );
                    }

                    return Optional.empty();
                };

        (
                (AbstractFurnaceBlockEntityAccessor)
                        (Object) this
        ).cosmicwill$setQuickCheck(
                combinedCheck
        );
    }

    public boolean isStellarRemnantInput() {
        return getItem(SLOT_INPUT).is(
                CWBlocks.STELLAR_REMNANT
                        .get()
                        .asItem()
        );
    }

    public void provideStellarHeat(
            int ticks
    ) {
        stellarHeatTicks = Math.max(
                stellarHeatTicks,
                Math.max(1, ticks)
        );
    }

    public void applyStellarHeat(
            int speedMultiplier
    ) {
        if (stellarHeatTicks > 0) {
            stellarHeatTicks--;

            if (!stellarHeatActive) {
                storedLitTime =
                        dataAccess.get(
                                DATA_LIT_TIME
                        );

                storedLitDuration =
                        dataAccess.get(
                                DATA_LIT_DURATION
                        );

                stellarHeatActive = true;
            }

            int saturatedDuration =
                    Math.max(
                            40,
                            speedMultiplier * 2
                    );

            dataAccess.set(
                    DATA_LIT_DURATION,
                    saturatedDuration
            );

            dataAccess.set(
                    DATA_LIT_TIME,
                    saturatedDuration
                            + speedMultiplier
            );

            return;
        }

        if (stellarHeatActive) {
            dataAccess.set(
                    DATA_LIT_TIME,
                    storedLitTime
            );

            dataAccess.set(
                    DATA_LIT_DURATION,
                    storedLitDuration
            );

            stellarHeatActive = false;
            storedLitTime = 0;
            storedLitDuration = 0;
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(
                "container.cosmicwill.compressed_furnace"
        );
    }

    @Override
    protected AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory
    ) {
        return new CompressedFurnaceMenu(
                containerId,
                inventory,
                this,
                this.dataAccess
        );
    }
}
