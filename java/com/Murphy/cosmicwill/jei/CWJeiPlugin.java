package com.Murphy.cosmicwill.jei;

import com.Murphy.cosmicwill.recipe.MatterCompressionRecipe;
import com.Murphy.cosmicwill.registry.CWBlocks;
import com.Murphy.cosmicwill.registry.CWRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public final class CWJeiPlugin implements IModPlugin {
    public static final RecipeType<MatterCompressionRecipe> MATTER_COMPRESSION =
            RecipeType.create(
                    "cosmicwill",
                    "matter_compressing",
                    MatterCompressionRecipe.class
            );

    private static final ResourceLocation PLUGIN_UID =
            new ResourceLocation("cosmicwill", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MatterCompressionRecipeCategory(
                        registration.getJeiHelpers().getGuiHelper()
                )
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        List<MatterCompressionRecipe> compressionRecipes =
                level.getRecipeManager().getAllRecipesFor(CWRecipeTypes.MATTER_COMPRESSING.get());

        registration.addRecipes(MATTER_COMPRESSION, compressionRecipes);

        /*
         * 原来的 BeaconPatternRecipe 手动注入已移除。
         * 压缩熔炉和两种强相互作用材料配方现在使用标准
         * minecraft:crafting_shaped JSON，JEI 会自动读取并展示。
         */
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                CWBlocks.MATTER_COMPRESSOR.get(),
                MATTER_COMPRESSION
        );

        /*
         * 保留原设定：压缩熔炉同时是普通熔炼和高炉熔炼的 JEI 催化剂。
         * 这里只影响 JEI 展示，不修改方块实体中的实际配方查找逻辑。
         */
        registration.addRecipeCatalyst(
                CWBlocks.COMPRESSED_FURNACE.get(),
                RecipeTypes.SMELTING
        );
        registration.addRecipeCatalyst(
                CWBlocks.COMPRESSED_FURNACE.get(),
                RecipeTypes.BLASTING
        );
    }
}
