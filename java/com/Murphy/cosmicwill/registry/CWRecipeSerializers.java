package com.Murphy.cosmicwill.registry;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.recipe.BeaconPatternRecipe;
import com.Murphy.cosmicwill.recipe.MatterCompressionRecipe;
import com.Murphy.cosmicwill.recipe.MatterCompressionRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CWRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>>
            RECIPE_SERIALIZERS =
            DeferredRegister.create(
                    ForgeRegistries.RECIPE_SERIALIZERS,
                    CustomWill.MODID
            );

    public static final RegistryObject<
            RecipeSerializer<MatterCompressionRecipe>
            > MATTER_COMPRESSING =
            RECIPE_SERIALIZERS.register(
                    "matter_compressing",
                    MatterCompressionRecipeSerializer::new
            );

    public static final RegistryObject<
            RecipeSerializer<BeaconPatternRecipe>
            > STRONG_MATERIAL_STAR_RING =
            RECIPE_SERIALIZERS.register(
                    "strong_material_star_ring",
                    () -> new SimpleCraftingRecipeSerializer<>(
                            (id, category) ->
                                    new BeaconPatternRecipe(
                                            id,
                                            category,
                                            BeaconPatternRecipe.Mode
                                                    .STRONG_MATERIAL_STAR_RING
                                    )
                    )
            );

    public static final RegistryObject<
            RecipeSerializer<BeaconPatternRecipe>
            > STRONG_MATERIAL_PRIMORDIUM_CROSS =
            RECIPE_SERIALIZERS.register(
                    "strong_material_primordium_cross",
                    () -> new SimpleCraftingRecipeSerializer<>(
                            (id, category) ->
                                    new BeaconPatternRecipe(
                                            id,
                                            category,
                                            BeaconPatternRecipe.Mode
                                                    .STRONG_MATERIAL_PRIMORDIUM_CROSS
                                    )
                    )
            );

    public static final RegistryObject<
            RecipeSerializer<BeaconPatternRecipe>
            > COMPRESSED_FURNACE_BEACON =
            RECIPE_SERIALIZERS.register(
                    "compressed_furnace_beacon",
                    () -> new SimpleCraftingRecipeSerializer<>(
                            (id, category) ->
                                    new BeaconPatternRecipe(
                                            id,
                                            category,
                                            BeaconPatternRecipe.Mode
                                                    .COMPRESSED_FURNACE
                                    )
                    )
            );

    private CWRecipeSerializers() {
    }
}
