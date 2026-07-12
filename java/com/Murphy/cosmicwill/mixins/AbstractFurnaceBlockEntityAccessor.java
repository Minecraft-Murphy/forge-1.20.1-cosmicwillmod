package com.Murphy.cosmicwill.mixins;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {

    @Mutable
    @Accessor("quickCheck")
    void cosmicwill$setQuickCheck(
            RecipeManager.CachedCheck<
                    Container,
                    ? extends AbstractCookingRecipe
                    > quickCheck
    );
}
