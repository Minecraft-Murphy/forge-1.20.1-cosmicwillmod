package com.Murphy.cosmicwill.registry;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.recipe.MatterCompressionRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CWRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(
                    ForgeRegistries.RECIPE_TYPES,
                    CustomWill.MODID
            );

    public static final RegistryObject<RecipeType<MatterCompressionRecipe>>
            MATTER_COMPRESSING =
            RECIPE_TYPES.register(
                    "matter_compressing",
                    () -> new RecipeType<>() {
                        @Override
                        public String toString() {
                            return CustomWill.MODID
                                    + ":matter_compressing";
                        }
                    }
            );

    private CWRecipeTypes() {
    }
}
